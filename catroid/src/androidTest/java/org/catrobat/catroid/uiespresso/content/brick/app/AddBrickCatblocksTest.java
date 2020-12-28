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

package org.catrobat.catroid.uiespresso.content.brick.app;

import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.WhenScript;
import org.catrobat.catroid.content.bricks.ChangeXByNBrick;
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick;
import org.catrobat.catroid.content.bricks.SetXBrick;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.recyclerview.fragment.CatblocksScriptFragment;
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment;
import org.catrobat.catroid.uiespresso.util.rules.FragmentActivityTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.meta.When;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;

import static androidx.test.espresso.web.assertion.WebViewAssertions.webContent;
import static androidx.test.espresso.web.matcher.DomMatchers.hasElementWithXpath;
import static androidx.test.espresso.web.sugar.Web.onWebView;

public class AddBrickCatblocksTest {
	@Rule
	public FragmentActivityTestRule<SpriteActivity> baseActivityTestRule = new
			FragmentActivityTestRule<>(SpriteActivity.class, SpriteActivity.EXTRA_FRAGMENT_POSITION,
			SpriteActivity.FRAGMENT_SCRIPTS);
	private WhenScript addedScript = new WhenScript();
	private Runnable addBricksAction = new Runnable() {
		@Override
		public void run() {
			synchronized (this) {
				View v = baseActivityTestRule.getActivity().findViewById(R.id.catblocksWebView);
				Fragment f = FragmentManager.findFragment(v);
				if (f instanceof CatblocksScriptFragment) {
					CatblocksScriptFragment catblocksFragment = (CatblocksScriptFragment) f;
					catblocksFragment.addBrick(addedScript.getScriptBrick());
					this.notify();
				}
			}
		}
	};

	@Before
	public void setUp() throws Exception {
		SettingsFragment.setUseCatBlocks(ApplicationProvider.getApplicationContext(), true);
		createProject();
		baseActivityTestRule.launchActivity();
	}

	@After
	public void tearDown() {
		baseActivityTestRule.getActivity().finish();
	}

	@Test
	public void testAddBricks() {
		synchronized (addBricksAction) {
			try {
				baseActivityTestRule.getActivity().runOnUiThread(addBricksAction);
				addBricksAction.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String strId = addedScript.getScriptId().toString().toLowerCase();
			int tryCounter = 0;
			// it may take some time until the brick appears in Catblocks, so try multiple times
			while (true) {
				tryCounter++;

				try {
					onWebView().check(webContent(hasElementWithXpath("//*[@data-id=\"" + strId + "\"]")));
					break;
				} catch (Throwable t) {
					if (tryCounter >= 25) {
						throw t;
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void createProject() {
		String projectName = getClass().getSimpleName();
		Project project = new Project(ApplicationProvider.getApplicationContext(), projectName);

		Sprite sprite = new Sprite("testSprite");
		project.getDefaultScene().addSprite(sprite);

		Script startScript = new StartScript();
		sprite.addScript(startScript);

		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentSprite(sprite);
	}
}
