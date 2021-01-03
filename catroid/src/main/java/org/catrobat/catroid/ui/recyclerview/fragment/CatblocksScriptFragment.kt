/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2020 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.ui.recyclerview.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.DummyBrick
import org.catrobat.catroid.content.bricks.ScriptBrickBaseType
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.ui.BottomBar
import org.catrobat.catroid.ui.fragment.AddBrickFragment
import org.catrobat.catroid.ui.fragment.BrickCategoryFragment
import org.catrobat.catroid.ui.fragment.BrickCategoryFragment.OnCategorySelectedListener
import org.catrobat.catroid.ui.fragment.UserDefinedBrickListFragment
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import org.catrobat.catroid.utils.SnackbarUtil
import org.json.JSONArray
import org.json.JSONObject
import org.koin.ext.getScopeName
import org.koin.java.KoinJavaComponent.inject
import java.util.Locale
import java.util.UUID

class CatblocksScriptFragment(
    private val currentScriptIndex: Int
) : Fragment(), OnCategorySelectedListener, AddBrickFragment.OnAddBrickListener {

    private var webview: WebView? = null

    companion object {
        val TAG: String = CatblocksScriptFragment::class.java.simpleName
    }

    private val projectManager = inject(ProjectManager::class.java).value

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.backpack).isVisible = false
        menu.findItem(R.id.copy).isVisible = false
        menu.findItem(R.id.delete).isVisible = false
        menu.findItem(R.id.rename).isVisible = false
        menu.findItem(R.id.show_details).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.catblocks) {
            activity?.runOnUiThread(SwitchTo1DHelper())

            return true
        } else if (item.itemId == R.id.catblocks_reorder_scripts) {
            val callback = ReorderCallback()
            webview!!.evaluateJavascript("javascript:CatBlocks.reorderCurrentScripts();", callback)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        BottomBar.showBottomBar(activity)

        setHasOptionsMenu(true)

        val view = View.inflate(activity, R.layout.fragment_catblocks, null)
        val webView = view.findViewById<WebView>(R.id.catblocksWebView)
        initWebView(webView)
        this.webview = webView
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(catblocksWebView: WebView) {
        catblocksWebView.settings.javaScriptEnabled = true

        if (BuildConfig.FEATURE_CATBLOCKS_DEBUGABLE) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireActivity()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireActivity()))
            .build()

        catblocksWebView.addJavascriptInterface(
            JSInterface(
                currentScriptIndex
            ), "Android"
        )

        catblocksWebView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
        catblocksWebView.loadUrl("https://appassets.androidplatform.net/assets/catblocks/index.html")
    }

    class ReorderCallback : ValueCallback<String> {

        override fun onReceiveValue(value: String?) { // do nothing
        }
    }

    inner class SwitchTo1DHelper : Runnable {

        var brickHelper: JSInterface.BrickHelper? = null

        override fun run() {
            SettingsFragment.setUseCatBlocks(context, false)

            var scriptFragment: ScriptFragment;
            if (brickHelper?.brick != null) {
                scriptFragment = ScriptFragment(brickHelper!!.brick)
            } else if (brickHelper != null && brickHelper!!.brickIsScript &&
                brickHelper?.script != null
            ) {
                scriptFragment = ScriptFragment(brickHelper!!.script)
            } else {
                scriptFragment = ScriptFragment()
            }

            val fragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction.replace(
                R.id.fragment_container, scriptFragment,
                ScriptFragment.TAG
            )
            fragmentTransaction.commit()
        }
    }

    inner class JSInterface(private val script: Int) {

        @JavascriptInterface
        fun getCurrentProject(): String {
            val projectXml = XstreamSerializer.getInstance()
                .getXmlAsStringFromProject(projectManager.currentProject)
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n$projectXml"
        }

        @JavascriptInterface
        fun getCurrentLanguage(): String =
            Locale.getDefault().toString().replace("_", "-")

        @JavascriptInterface
        fun isRTL(): Boolean {
            val directionality = Character.getDirectionality(Locale.getDefault().displayName[0])
            return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
        }

        @JavascriptInterface
        fun getSceneNameToDisplay(): String? {
            return projectManager.currentlyEditedScene?.name?.trim()
        }

        @JavascriptInterface
        fun getSpriteNameToDisplay(): String? {
            return projectManager.currentSprite?.name?.trim()
        }

        @JavascriptInterface
        fun getScriptIndexToDisplay(): Int = script

        @SuppressLint
        @JavascriptInterface
        fun updateScriptPosition(strScriptId: String, x: String, y: String) {
            if (projectManager.currentProject == null) {
                return
            }

            val scriptId = UUID.fromString(strScriptId)
            var posX: Float = x.toFloat()
            var posY: Float = y.toFloat()

            for (scene in projectManager.currentProject.sceneList) {
                if (updateScriptPositionInScene(scriptId, posX, posY, scene)) {
                    return
                }
            }
        }

        // region update script position helpers
        private fun updateScriptPositionInScene(
            scriptId: UUID,
            x: Float,
            y: Float,
            scene: Scene
        ): Boolean {
            for (sprite in scene.spriteList) {
                if (updateScriptPositionInSprite(scriptId, x, y, sprite)) {
                    return true
                }
            }
            return false
        }

        private fun updateScriptPositionInSprite(
            scriptId: UUID,
            x: Float,
            y: Float,
            sprite: Sprite
        ): Boolean {
            for (script in sprite.scriptList) {
                if (script.scriptId == scriptId) {
                    script.posX = x
                    script.posY = y
                    return true
                }
            }
            return false
        }
        // endregion

        @JavascriptInterface
        fun addDummyBrick(brickStrIdsToMove: Array<String>): String {
            val dummyBrick = DummyBrick()

            val brickIdsToMove = mutableListOf<UUID>()
            for (strId in brickStrIdsToMove) {
                brickIdsToMove.add(UUID.fromString(strId))
            }

            for (script in projectManager.currentSprite.scriptList) {

                val foundBricks = script
                    .removeBricksFromScript(brickIdsToMove)

                if (foundBricks != null) {
                    dummyBrick.script.brickList.addAll(foundBricks)
                    break
                }
            }

            projectManager.currentSprite.scriptList.add(dummyBrick.script)
            return dummyBrick.script.scriptId.toString()
        }

        @JavascriptInterface
        fun moveBricksToScript(
            newParentStrId: String,
            parentSubStackIdx: Int,
            brickStrIdsToMove: Array<String>
        ): Boolean {
            val brickIdsToMove = mutableListOf<UUID>()
            for (strId in brickStrIdsToMove) {
                brickIdsToMove.add(UUID.fromString(strId))
            }

            val newParentId = UUID.fromString(newParentStrId)

            var bricksToMove: List<Brick>? = null
            for (script in projectManager.currentSprite.scriptList) {
                bricksToMove = script.removeBricksFromScript(brickIdsToMove)
                if (bricksToMove != null) {
                    break
                }
            }

            if (bricksToMove == null) {
                return false
            }

            println(bricksToMove.size)

            for (script in projectManager.currentSprite.scriptList) {
                if (script.insertBrickAfter(newParentId, parentSubStackIdx, bricksToMove)) {
                    return true
                }
            }

            return false
        }

        @JavascriptInterface
        fun removeEmptyDummyBricks(): String {
            val removed = projectManager.currentSprite.removeAllEmptyDummyScripts();
            return JSONArray(removed).toString()
        }

        @JavascriptInterface
        fun switchTo1D(strClickedBrickId: String) {
            val brickId = UUID.fromString(strClickedBrickId)

            var brickHelper =
                locateBrickInSprite(projectManager.currentSprite, brickId)

            if (brickHelper != null) {
                val switchTo1DHelper = SwitchTo1DHelper()
                switchTo1DHelper.brickHelper = brickHelper
                activity?.runOnUiThread(switchTo1DHelper)
            }
        }

        inner class BrickHelper {
            var brickIsScript: Boolean = false
            var script: Script? = null
            var brick: Brick? = null
        }

        private fun locateBrickInSprite(sprite: Sprite, brickId: UUID): BrickHelper? {
            for (script in sprite.scriptList) {

                if (script.scriptId == brickId) {
                    val brickHelper = BrickHelper()
                    brickHelper.brickIsScript = true
                    brickHelper.script = script
                    return brickHelper
                }

                val tmpBrickHelper =
                    locateBrickInScript(brickId, script)
                if (tmpBrickHelper?.brick != null) {
                    tmpBrickHelper.script = script
                    return tmpBrickHelper
                }
            }
            return null
        }

        private fun locateBrickInScript(brickId: UUID, script: Script): BrickHelper? {
            for (brick in script.brickList) {
                val brickHelper = checkBrick(brickId, brick)
                if (brickHelper != null) {
                    return brickHelper
                }
            }
            return null
        }

        private fun checkBrick(brickId: UUID, brick: Brick): BrickHelper? {
            if (brick.brickID == brickId) {
                val brickHelper = BrickHelper()
                brickHelper.brick = brick
                return brickHelper
            }

            if (brick !is CompositeBrick) {
                return null
            }

            val compositeBrick = brick as CompositeBrick
            for (childBrick in compositeBrick.nestedBricks) {
                val brickHelper = checkBrick(brickId, childBrick)
                if (brickHelper != null) {
                    return brickHelper
                }
            }

            if (!compositeBrick.hasSecondaryList()) {
                return null
            }

            for (childBrick in compositeBrick.secondaryNestedBricks) {
                val brickHelper = checkBrick(brickId, childBrick)
                if (brickHelper != null) {
                    return brickHelper
                }
            }

            return null
        }

        @JavascriptInterface
        fun removeBricks(brickStrIdsToRemove: Array<String>) {
            val brickIdsToRemove = arrayListOf<UUID>()
            for (strId in brickStrIdsToRemove) {
                brickIdsToRemove.add(UUID.fromString(strId))
            }

            for (script in projectManager.currentSprite.scriptList) {
                if (brickIdsToRemove.contains(script.scriptId)) {
                    projectManager.currentSprite.scriptList.remove(script)
                    break
                }
                if (script.removeBricksFromScript(brickIdsToRemove) != null) {
                    break
                }
            }
        }

        @JavascriptInterface
        fun duplicateBrick(brickStrIdToClone: String): String? {
            val brickIdToClone = UUID.fromString(brickStrIdToClone)

            val foundBrick =
                locateBrickInSprite(projectManager.currentSprite, brickIdToClone)
                    ?: return null

            var oldNewIds: Map<UUID, UUID>

            if(foundBrick.brickIsScript) {
                val clone = foundBrick.script?.clone() ?: return null
                projectManager.currentSprite.scriptList.add(clone)
                return clone.scriptId.toString()

            } else {
                val clone = foundBrick.brick?.clone() ?: return null
                val dummyBrick = DummyBrick()
                dummyBrick.script.brickList.add(clone)
                projectManager.currentSprite.scriptList.add(dummyBrick.script)
                return dummyBrick.script.scriptId.toString()
            }
        }
    }

    fun handleAddButton() {
        val brickCategoryFragment = BrickCategoryFragment()
        brickCategoryFragment.setOnCategorySelectedListener(this)

        parentFragmentManager.beginTransaction()
            .add(
                R.id.fragment_container,
                brickCategoryFragment,
                BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG
            )
            .addToBackStack(BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG)
            .commit()

        SnackbarUtil.showHintSnackbar(activity, R.string.hint_category)
    }

    override fun onCategorySelected(category: String?) {
        var addListFragment: ListFragment
        var tag: String? = ""

        if (category == requireContext().getString(R.string.category_user_bricks)) {
            addListFragment = UserDefinedBrickListFragment.newInstance(this)
            tag = UserDefinedBrickListFragment.USER_DEFINED_BRICK_LIST_FRAGMENT_TAG
        } else {
            addListFragment = AddBrickFragment.newInstance(category, this)
            tag = AddBrickFragment.ADD_BRICK_FRAGMENT_TAG
        }

        parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, addListFragment, tag)
            .addToBackStack(null)
            .commit()
    }

    override fun addBrick(brick: Brick?) {
        if (brick == null) {
            return
        }

        val addedBricks = arrayListOf<BrickInfoHolder>()

        if (brick is ScriptBrickBaseType) {
            projectManager.currentSprite.scriptList.add(brick.script)
            addedBricks.add(
                BrickInfoHolder(
                    brick.script.scriptId.toString(),
                    brick.script.javaClass.simpleName
                )
            )
        } else {
            val dummyBrick = DummyBrick()
            dummyBrick.script.brickList.add(brick)
            projectManager.currentSprite.scriptList.add(dummyBrick.script)

            addedBricks.add(
                BrickInfoHolder(
                    dummyBrick.script.scriptId.toString(),
                    dummyBrick.script.javaClass.simpleName
                )
            )

            addedBricks.add(BrickInfoHolder(brick.brickID.toString(), brick.javaClass.simpleName))
        }

        val addedBricksString = Gson().toJson(addedBricks)
        webview!!.evaluateJavascript("javascript:CatBlocks.addBricks($addedBricksString);", null)
    }

    private class BrickInfoHolder(brickId: String, brickType: String) {
        @SerializedName("brickId")
        val brickId: String = brickId

        @SerializedName("brickType")
        val brickType: String = brickType
    }
}
