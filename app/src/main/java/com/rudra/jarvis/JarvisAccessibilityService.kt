package com.rudra.jarvis

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for Version 2.2
    }

    override fun onInterrupt() {
        // Not needed for Version 2.2
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun openRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    

    fun openNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }
    fun tapPercent(xPercent: Float, yPercent: Float) {
    val metrics = resources.displayMetrics
    val x = metrics.widthPixels * xPercent
    val y = metrics.heightPixels * yPercent
    tap(x, y)
}

fun typeText(text: String) {
    val root = rootInActiveWindow ?: return
    val editText = findEditableNode(root)

    if (editText != null) {
        val args = android.os.Bundle()
        args.putCharSequence(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )

        editText.performAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )
    }
}

private fun findEditableNode(
    node: android.view.accessibility.AccessibilityNodeInfo?
): android.view.accessibility.AccessibilityNodeInfo? {

    if (node == null) return null

    if (node.isEditable) return node

    for (i in 0 until node.childCount) {
        val result = findEditableNode(node.getChild(i))

        if (result != null) {
            return result
        }
    }

    return null
}

    fun scrollDown() {
        swipe(500f, 1500f, 500f, 500f)
    }

    fun scrollUp() {
        swipe(500f, 500f, 500f, 1500f)
    }

    fun tapCenter() {
        tap(500f, 1000f)
    }

    private fun tap(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
            .build()

        dispatchGesture(gesture, null, null)
    }
}
