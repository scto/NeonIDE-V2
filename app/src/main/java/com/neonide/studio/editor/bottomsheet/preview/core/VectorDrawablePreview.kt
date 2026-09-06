package com.neonide.studio.editor.bottomsheet.preview.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.Xml
import androidx.core.graphics.PathParser
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser

/**
 * Renders editor/project `<vector>` XML to a Bitmap.
 *
 * Platform [android.graphics.drawable.VectorDrawable] only inflates compiled resource XML
 * (`XmlBlock$Parser`), so free-form editor XML must be parsed and drawn manually.
 *
 * Supports root vector attrs, nested groups/transforms, clip-path, path fill/stroke styles,
 * trimPath, and linear/radial/sweep gradients (including aapt:attr nested form).
 */
object VectorDrawablePreview {
    private const val MAX_PX = 1024
    private const val DEFAULT_DP = 80f
    private const val DEFAULT_VIEWPORT = 24f
    private const val PAD_PX = 24
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private const val AAPT_NS = "http://schemas.android.com/aapt"

    fun render(context: Context, xmlContent: String, projectDir: java.io.File? = null): Bitmap {
        val colorResolver = ProjectColorResolver(projectDir)
        val model = parseVector(xmlContent, colorResolver)

        val density = context.resources.displayMetrics.density
        val defaultPx = (DEFAULT_DP * density).toInt().coerceAtLeast(1)

        val intrinsicW = model.widthPx(density).takeIf { it > 0 } ?: defaultPx
        val intrinsicH = model.heightPx(density).takeIf { it > 0 } ?: defaultPx

        val w = intrinsicW.coerceIn(1, MAX_PX)
        val h = intrinsicH.coerceIn(1, MAX_PX)

        val outW = (w + PAD_PX * 2).coerceAtMost(MAX_PX + PAD_PX * 2)
        val outH = (h + PAD_PX * 2).coerceAtMost(MAX_PX + PAD_PX * 2)

        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCheckerboard(canvas, outW, outH)

        val left = (outW - w) / 2f
        val top = (outH - h) / 2f

        canvas.save()
        canvas.translate(left, top)
        if (model.autoMirrored) {
            // Preview always uses LTR frame; mirror when autoMirrored is set.
            canvas.translate(w.toFloat(), 0f)
            canvas.scale(-1f, 1f)
        }
        canvas.scale(
            w / model.viewportWidth.coerceAtLeast(0.001f),
            h / model.viewportHeight.coerceAtLeast(0.001f)
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val tintFilter = model.tintColor?.let { PorterDuffColorFilter(it, model.tintMode) }
        drawNode(
            canvas,
            model.root,
            DrawState(
                rootAlpha = model.alpha,
                paint = paint,
                tintFilter = tintFilter
            )
        )
        canvas.restore()
        return bitmap
    }

    private fun drawNode(canvas: Canvas, node: VectorNode, state: DrawState) {
        when (node) {
            is VectorNode.Group -> drawGroup(canvas, node, state)
            is VectorNode.PathNode -> drawPath(canvas, node, state)
            is VectorNode.ClipPath -> Unit
        }
    }

    private fun drawGroup(canvas: Canvas, group: VectorNode.Group, state: DrawState) {
        canvas.save()
        val matrix = Matrix()
        // Match VectorDrawable VGroup transform composition order.
        matrix.postTranslate(-group.pivotX, -group.pivotY)
        matrix.postScale(group.scaleX, group.scaleY)
        if (group.rotation != 0f) {
            matrix.postRotate(group.rotation)
        }
        matrix.postTranslate(group.translateX + group.pivotX, group.translateY + group.pivotY)
        canvas.concat(matrix)

        val clipPaths = group.children.filterIsInstance<VectorNode.ClipPath>()
        if (clipPaths.isNotEmpty()) {
            val clip = Path()
            for (cp in clipPaths) {
                val p = pathFromData(cp.pathData) ?: continue
                p.fillType = if (cp.fillTypeEvenOdd) {
                    Path.FillType.EVEN_ODD
                } else {
                    Path.FillType.WINDING
                }
                clip.addPath(p)
            }
            canvas.clipPath(clip)
        }

        for (child in group.children) {
            if (child is VectorNode.ClipPath) continue
            drawNode(canvas, child, state)
        }
        canvas.restore()
    }

    private fun drawPath(canvas: Canvas, pathNode: VectorNode.PathNode, state: DrawState) {
        val basePath = pathFromData(pathNode.pathData) ?: return
        basePath.fillType = if (pathNode.fillTypeEvenOdd) {
            Path.FillType.EVEN_ODD
        } else {
            Path.FillType.WINDING
        }
        val path = applyTrimPath(basePath, pathNode) ?: return

        val fillAlpha = (pathNode.fillAlpha * state.rootAlpha).coerceIn(0f, 1f)
        val strokeAlpha = (pathNode.strokeAlpha * state.rootAlpha).coerceIn(0f, 1f)

        when (val fill = pathNode.fill) {
            is ColorValue.Solid -> {
                if (fillAlpha > 0f && fill.color != Color.TRANSPARENT) {
                    // setColor overwrites alpha; apply fillAlpha after color.
                    preparePaint(state, Paint.Style.FILL)
                    state.paint.color = fill.color
                    state.paint.alpha = combineAlpha(fill.color, fillAlpha)
                    state.paint.shader = null
                    canvas.drawPath(path, state.paint)
                }
            }
            is ColorValue.Gradient -> {
                if (fillAlpha > 0f) {
                    preparePaint(state, Paint.Style.FILL)
                    state.paint.alpha = (fillAlpha * 255f).toInt().coerceIn(0, 255)
                    state.paint.shader = createShader(fill, path)
                    canvas.drawPath(path, state.paint)
                }
            }
            ColorValue.None -> Unit
        }

        if (pathNode.strokeWidth > 0f && strokeAlpha > 0f) {
            when (val stroke = pathNode.stroke) {
                is ColorValue.Solid -> {
                    if (stroke.color != Color.TRANSPARENT) {
                        preparePaint(state, Paint.Style.STROKE)
                        state.paint.color = stroke.color
                        state.paint.alpha = combineAlpha(stroke.color, strokeAlpha)
                        state.paint.strokeWidth = pathNode.strokeWidth
                        state.paint.strokeCap = pathNode.strokeCap
                        state.paint.strokeJoin = pathNode.strokeJoin
                        state.paint.strokeMiter = pathNode.strokeMiterLimit
                        state.paint.shader = null
                        canvas.drawPath(path, state.paint)
                    }
                }
                is ColorValue.Gradient -> {
                    preparePaint(state, Paint.Style.STROKE)
                    state.paint.alpha = (strokeAlpha * 255f).toInt().coerceIn(0, 255)
                    state.paint.strokeWidth = pathNode.strokeWidth
                    state.paint.strokeCap = pathNode.strokeCap
                    state.paint.strokeJoin = pathNode.strokeJoin
                    state.paint.strokeMiter = pathNode.strokeMiterLimit
                    state.paint.shader = createShader(stroke, path)
                    canvas.drawPath(path, state.paint)
                }
                ColorValue.None -> Unit
            }
        }
    }

    private fun preparePaint(state: DrawState, style: Paint.Style) {
        state.paint.reset()
        state.paint.isAntiAlias = true
        state.paint.style = style
        state.paint.colorFilter = state.tintFilter
    }

    private fun combineAlpha(color: Int, alpha: Float): Int {
        val colorA = Color.alpha(color) / 255f
        return ((colorA * alpha) * 255f).toInt().coerceIn(0, 255)
    }

    private fun applyTrimPath(base: Path, node: VectorNode.PathNode): Path? {
        val start = node.trimPathStart.coerceIn(0f, 1f)
        val end = node.trimPathEnd.coerceIn(0f, 1f)
        val offset = node.trimPathOffset
        if (start == 0f && end == 1f && offset == 0f) {
            return base
        }
        if (start == end) {
            return null
        }

        val out = Path()
        val measureContour = PathMeasure(base, false)
        var more = true
        while (more) {
            val contourLen = measureContour.length
            if (contourLen > 0f) {
                val s = ((start + offset) % 1f + 1f) % 1f
                val e = ((end + offset) % 1f + 1f) % 1f
                val startD = s * contourLen
                val endD = e * contourLen
                if (s < e) {
                    measureContour.getSegment(startD, endD, out, true)
                } else if (s > e) {
                    measureContour.getSegment(startD, contourLen, out, true)
                    measureContour.getSegment(0f, endD, out, true)
                }
            }
            more = measureContour.nextContour()
        }
        out.fillType = base.fillType
        return out
    }

    private fun createShader(gradient: ColorValue.Gradient, path: Path): Shader? {
        if (gradient.colors.isEmpty()) return null
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val colors = gradient.colors
        val positions = gradient.offsets
        val tile = gradient.tileMode
        return when (gradient.type) {
            GradientType.LINEAR -> LinearGradient(
                gradient.startX,
                gradient.startY,
                gradient.endX,
                gradient.endY,
                colors,
                positions,
                tile
            )
            GradientType.RADIAL -> {
                val radius = gradient.gradientRadius.takeIf { it > 0f }
                    ?: (maxOf(bounds.width(), bounds.height()) / 2f).coerceAtLeast(0.001f)
                RadialGradient(
                    gradient.centerX,
                    gradient.centerY,
                    radius,
                    colors,
                    positions,
                    tile
                )
            }
            GradientType.SWEEP -> SweepGradient(
                gradient.centerX,
                gradient.centerY,
                colors,
                positions
            )
        }
    }

    private fun pathFromData(pathData: String): Path? {
        if (pathData.isBlank()) return null
        return try {
            PathParser.createPathFromPathData(pathData)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawCheckerboard(canvas: Canvas, width: Int, height: Int) {
        val light = 0xFF808080.toInt()
        val dark = 0xFFA8A8A8.toInt()
        val cell = 50
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = 0
        while (y < height) {
            var x = 0
            val row = y / cell
            while (x < width) {
                val col = x / cell
                paint.color = if ((row + col) % 2 == 0) light else dark
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    minOf(x + cell, width).toFloat(),
                    minOf(y + cell, height).toFloat(),
                    paint
                )
                x += cell
            }
            y += cell
        }
    }

    private fun parseVector(xmlContent: String, colorResolver: ProjectColorResolver): VectorModel {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xmlContent))

        var event = parser.eventType
        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) {
            event = parser.next()
        }
        if (event != XmlPullParser.START_TAG) {
            throw IllegalStateException("No start tag found in vector XML")
        }

        val root = localName(parser.name)
        if (root != "vector") {
            throw IllegalStateException("Expected root <vector>, found <$root>")
        }

        val widthDp = parseDimensionDp(attr(parser, "width")) ?: DEFAULT_DP
        val heightDp = parseDimensionDp(attr(parser, "height")) ?: DEFAULT_DP
        val viewportW = attr(parser, "viewportWidth")?.toFloatOrNull() ?: DEFAULT_VIEWPORT
        val viewportH = attr(parser, "viewportHeight")?.toFloatOrNull() ?: DEFAULT_VIEWPORT
        val alpha = attr(parser, "alpha")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
        val autoMirrored = attr(parser, "autoMirrored")?.equals("true", true) == true
        val tintColor = parseColor(attr(parser, "tint"), colorResolver)
        val tintMode = parseTintMode(attr(parser, "tintMode"))

        val children = parseChildNodes(parser, colorResolver)
        if (children.none { hasDrawablePath(it) }) {
            throw IllegalStateException("Vector has no drawable <path> elements")
        }

        return VectorModel(
            widthDp = widthDp,
            heightDp = heightDp,
            viewportWidth = viewportW.coerceAtLeast(0.001f),
            viewportHeight = viewportH.coerceAtLeast(0.001f),
            alpha = alpha,
            autoMirrored = autoMirrored,
            tintColor = tintColor,
            tintMode = tintMode,
            root = VectorNode.Group(
                name = null,
                rotation = 0f,
                pivotX = 0f,
                pivotY = 0f,
                scaleX = 1f,
                scaleY = 1f,
                translateX = 0f,
                translateY = 0f,
                children = children
            )
        )
    }

    private fun hasDrawablePath(node: VectorNode): Boolean = when (node) {
        is VectorNode.PathNode -> true
        is VectorNode.Group -> node.children.any { hasDrawablePath(it) }
        is VectorNode.ClipPath -> false
    }

    /**
     * Parses child nodes of the current open tag until its END_TAG.
     * Caller must be positioned on the parent START_TAG already consumed.
     */
    private fun parseChildNodes(
        parser: XmlPullParser,
        resolver: ProjectColorResolver
    ): List<VectorNode> {
        val children = mutableListOf<VectorNode>()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    when (localName(parser.name)) {
                        "group" -> children.add(parseGroup(parser, resolver))
                        "path" -> parsePath(parser, resolver)?.let { children.add(it) }
                        "clip-path" -> {
                            // Read attrs while on START_TAG, then always consume to END_TAG.
                            parseClipPath(parser)?.let { children.add(it) }
                            skipSubtree(parser)
                        }
                        else -> skipSubtree(parser)
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return children
    }

    private fun parseGroup(
        parser: XmlPullParser,
        resolver: ProjectColorResolver
    ): VectorNode.Group {
        val name = attr(parser, "name")
        val rotation = attr(parser, "rotation")?.toFloatOrNull() ?: 0f
        val pivotX = attr(parser, "pivotX")?.toFloatOrNull() ?: 0f
        val pivotY = attr(parser, "pivotY")?.toFloatOrNull() ?: 0f
        val scaleX = attr(parser, "scaleX")?.toFloatOrNull() ?: 1f
        val scaleY = attr(parser, "scaleY")?.toFloatOrNull() ?: 1f
        val translateX = attr(parser, "translateX")?.toFloatOrNull() ?: 0f
        val translateY = attr(parser, "translateY")?.toFloatOrNull() ?: 0f
        val children = parseChildNodes(parser, resolver)
        return VectorNode.Group(
            name = name,
            rotation = rotation,
            pivotX = pivotX,
            pivotY = pivotY,
            scaleX = scaleX,
            scaleY = scaleY,
            translateX = translateX,
            translateY = translateY,
            children = children
        )
    }

    private fun parseClipPath(parser: XmlPullParser): VectorNode.ClipPath? {
        val pathData = attr(parser, "pathData")?.trim().orEmpty()
        if (pathData.isEmpty()) return null
        val fillTypeEvenOdd =
            attr(parser, "fillType")?.equals("evenOdd", ignoreCase = true) == true
        return VectorNode.ClipPath(
            name = attr(parser, "name"),
            pathData = pathData,
            fillTypeEvenOdd = fillTypeEvenOdd
        )
    }

    private fun parsePath(
        parser: XmlPullParser,
        resolver: ProjectColorResolver
    ): VectorNode.PathNode? {
        val pathData = attr(parser, "pathData")?.trim().orEmpty()
        if (pathData.isEmpty()) {
            skipSubtree(parser)
            return null
        }

        var fill: ColorValue = solidOrNone(attr(parser, "fillColor"), resolver)
        var stroke: ColorValue = solidOrNone(attr(parser, "strokeColor"), resolver)
        val fillAlpha = (attr(parser, "fillAlpha")?.toFloatOrNull() ?: 1f).coerceIn(0f, 1f)
        val strokeAlpha = (attr(parser, "strokeAlpha")?.toFloatOrNull() ?: 1f).coerceIn(0f, 1f)
        val strokeWidth = attr(parser, "strokeWidth")?.toFloatOrNull() ?: 0f
        val fillTypeEvenOdd =
            attr(parser, "fillType")?.equals("evenOdd", ignoreCase = true) == true
        val strokeCap = parseStrokeCap(attr(parser, "strokeLineCap"))
        val strokeJoin = parseStrokeJoin(attr(parser, "strokeLineJoin"))
        val strokeMiterLimit = attr(parser, "strokeMiterLimit")?.toFloatOrNull() ?: 4f
        val trimStart = attr(parser, "trimPathStart")?.toFloatOrNull() ?: 0f
        val trimEnd = attr(parser, "trimPathEnd")?.toFloatOrNull() ?: 1f
        val trimOffset = attr(parser, "trimPathOffset")?.toFloatOrNull() ?: 0f
        val name = attr(parser, "name")

        val nested = parsePathNestedContent(parser, resolver)
        if (nested.fill != null) fill = nested.fill
        if (nested.stroke != null) stroke = nested.stroke

        return VectorNode.PathNode(
            name = name,
            pathData = pathData,
            fill = fill,
            fillAlpha = fillAlpha,
            stroke = stroke,
            strokeAlpha = strokeAlpha,
            strokeWidth = strokeWidth,
            fillTypeEvenOdd = fillTypeEvenOdd,
            strokeCap = strokeCap,
            strokeJoin = strokeJoin,
            strokeMiterLimit = strokeMiterLimit,
            trimPathStart = trimStart,
            trimPathEnd = trimEnd,
            trimPathOffset = trimOffset
        )
    }

    private data class NestedPaint(val fill: ColorValue? = null, val stroke: ColorValue? = null)

    /**
     * Consumes path children until path END_TAG.
     * Supports aapt:attr fillColor/strokeColor wrapping gradient.
     */
    private fun parsePathNestedContent(
        parser: XmlPullParser,
        resolver: ProjectColorResolver
    ): NestedPaint {
        var fill: ColorValue? = null
        var stroke: ColorValue? = null
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    val tag = localName(parser.name)
                    val ns = parser.namespace
                    if (tag == "attr" && (ns == AAPT_NS || parser.prefix == "aapt")) {
                        val attrName = parser.getAttributeValue(null, "name")
                            ?: parser.getAttributeValue(ANDROID_NS, "name")
                            ?: attrAny(parser, "name")
                        val gradient = parseAaptAttrGradient(parser, resolver)
                        when {
                            attrName?.endsWith("fillColor") == true && gradient != null -> {
                                fill = gradient
                            }
                            attrName?.endsWith("strokeColor") == true && gradient != null -> {
                                stroke = gradient
                            }
                        }
                    } else if (tag == "gradient") {
                        val g = parseGradientElement(parser, resolver)
                        if (fill == null) fill = g
                    } else {
                        skipSubtree(parser)
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return NestedPaint(fill, stroke)
    }

    private fun parseAaptAttrGradient(
        parser: XmlPullParser,
        resolver: ProjectColorResolver
    ): ColorValue.Gradient? {
        var gradient: ColorValue.Gradient? = null
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (localName(parser.name) == "gradient") {
                        gradient = parseGradientElement(parser, resolver)
                    } else {
                        skipSubtree(parser)
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return gradient
    }

    private fun parseGradientElement(
        parser: XmlPullParser,
        resolver: ProjectColorResolver
    ): ColorValue.Gradient? {
        val type = when (attr(parser, "type")?.lowercase()) {
            "radial" -> GradientType.RADIAL
            "sweep" -> GradientType.SWEEP
            else -> GradientType.LINEAR
        }
        val startX = attr(parser, "startX")?.toFloatOrNull() ?: 0f
        val startY = attr(parser, "startY")?.toFloatOrNull() ?: 0f
        val endX = attr(parser, "endX")?.toFloatOrNull() ?: 0f
        val endY = attr(parser, "endY")?.toFloatOrNull() ?: 0f
        val centerX = attr(parser, "centerX")?.toFloatOrNull() ?: 0f
        val centerY = attr(parser, "centerY")?.toFloatOrNull() ?: 0f
        val gradientRadius = attr(parser, "gradientRadius")?.toFloatOrNull() ?: 0f
        val tileMode = parseTileMode(attr(parser, "tileMode"))
        val startColor = parseColor(attr(parser, "startColor"), resolver)
        val centerColor = parseColor(attr(parser, "centerColor"), resolver)
        val endColor = parseColor(attr(parser, "endColor"), resolver)

        val items = mutableListOf<Pair<Int, Float>>()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (localName(parser.name) == "item") {
                        val color = parseColor(attr(parser, "color"), resolver)
                        val offset = attr(parser, "offset")?.toFloatOrNull()
                        if (color != null && offset != null) {
                            items.add(color to offset.coerceIn(0f, 1f))
                        }
                        skipSubtree(parser)
                    } else {
                        skipSubtree(parser)
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        if (items.isEmpty()) {
            if (startColor != null) items.add(startColor to 0f)
            if (centerColor != null) items.add(centerColor to 0.5f)
            if (endColor != null) items.add(endColor to 1f)
        }
        if (items.isEmpty()) return null

        items.sortBy { it.second }
        val colors = IntArray(items.size) { items[it].first }
        val offsets = FloatArray(items.size) { items[it].second }

        return ColorValue.Gradient(
            type = type,
            colors = colors,
            offsets = offsets,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            centerX = centerX,
            centerY = centerY,
            gradientRadius = gradientRadius,
            tileMode = tileMode
        )
    }

    private fun skipSubtree(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    private fun solidOrNone(raw: String?, resolver: ProjectColorResolver): ColorValue {
        val color = parseColor(raw, resolver) ?: return ColorValue.None
        return ColorValue.Solid(color)
    }

    private fun parseColor(raw: String?, resolver: ProjectColorResolver): Int? =
        resolver.resolve(raw)

    private fun parseStrokeCap(raw: String?): Paint.Cap = when (raw?.lowercase()) {
        "round" -> Paint.Cap.ROUND
        "square" -> Paint.Cap.SQUARE
        else -> Paint.Cap.BUTT
    }

    private fun parseStrokeJoin(raw: String?): Paint.Join = when (raw?.lowercase()) {
        "round" -> Paint.Join.ROUND
        "bevel" -> Paint.Join.BEVEL
        else -> Paint.Join.MITER
    }

    private fun parseTileMode(raw: String?): Shader.TileMode = when (raw?.lowercase()) {
        "repeat" -> Shader.TileMode.REPEAT
        "mirror" -> Shader.TileMode.MIRROR
        else -> Shader.TileMode.CLAMP
    }

    private fun parseTintMode(raw: String?): PorterDuff.Mode = when (raw?.lowercase()) {
        "src_over" -> PorterDuff.Mode.SRC_OVER
        "src_in" -> PorterDuff.Mode.SRC_IN
        "src_atop" -> PorterDuff.Mode.SRC_ATOP
        "multiply" -> PorterDuff.Mode.MULTIPLY
        "screen" -> PorterDuff.Mode.SCREEN
        "add" -> PorterDuff.Mode.ADD
        else -> PorterDuff.Mode.SRC_IN
    }

    private fun attr(parser: XmlPullParser, name: String): String? {
        parser.getAttributeValue(ANDROID_NS, name)?.let { return it }
        for (i in 0 until parser.attributeCount) {
            val local = parser.getAttributeName(i)?.substringAfterLast(':')
            if (local == name) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private fun attrAny(parser: XmlPullParser, name: String): String? {
        for (i in 0 until parser.attributeCount) {
            val local = parser.getAttributeName(i)?.substringAfterLast(':')
            if (local == name) return parser.getAttributeValue(i)
        }
        return null
    }

    private fun localName(name: String?): String = name?.substringAfterLast(':').orEmpty()

    private fun parseDimensionDp(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val v = raw.trim()
        return when {
            v.endsWith("dp", ignoreCase = true) -> v.dropLast(2).toFloatOrNull()
            v.endsWith("dip", ignoreCase = true) -> v.dropLast(3).toFloatOrNull()
            v.endsWith("px", ignoreCase = true) -> v.dropLast(2).toFloatOrNull()
            v.endsWith("sp", ignoreCase = true) -> v.dropLast(2).toFloatOrNull()
            else -> v.toFloatOrNull()
        }
    }

    private data class DrawState(
        val rootAlpha: Float,
        val paint: Paint,
        val tintFilter: PorterDuffColorFilter?
    )

    private data class VectorModel(
        val widthDp: Float,
        val heightDp: Float,
        val viewportWidth: Float,
        val viewportHeight: Float,
        val alpha: Float,
        val autoMirrored: Boolean,
        val tintColor: Int?,
        val tintMode: PorterDuff.Mode,
        val root: VectorNode.Group
    ) {
        fun widthPx(density: Float): Int = (widthDp * density).toInt().coerceAtLeast(1)

        fun heightPx(density: Float): Int = (heightDp * density).toInt().coerceAtLeast(1)
    }

    private sealed class VectorNode {
        data class Group(
            val name: String?,
            val rotation: Float,
            val pivotX: Float,
            val pivotY: Float,
            val scaleX: Float,
            val scaleY: Float,
            val translateX: Float,
            val translateY: Float,
            val children: List<VectorNode>
        ) : VectorNode()

        data class PathNode(
            val name: String?,
            val pathData: String,
            val fill: ColorValue,
            val fillAlpha: Float,
            val stroke: ColorValue,
            val strokeAlpha: Float,
            val strokeWidth: Float,
            val fillTypeEvenOdd: Boolean,
            val strokeCap: Paint.Cap,
            val strokeJoin: Paint.Join,
            val strokeMiterLimit: Float,
            val trimPathStart: Float,
            val trimPathEnd: Float,
            val trimPathOffset: Float
        ) : VectorNode()

        data class ClipPath(val name: String?, val pathData: String, val fillTypeEvenOdd: Boolean) :
            VectorNode()
    }

    private enum class GradientType {
        LINEAR,
        RADIAL,
        SWEEP
    }

    private sealed class ColorValue {
        data object None : ColorValue()
        data class Solid(val color: Int) : ColorValue()
        data class Gradient(
            val type: GradientType,
            val colors: IntArray,
            val offsets: FloatArray,
            val startX: Float,
            val startY: Float,
            val endX: Float,
            val endY: Float,
            val centerX: Float,
            val centerY: Float,
            val gradientRadius: Float,
            val tileMode: Shader.TileMode
        ) : ColorValue()
    }
}
