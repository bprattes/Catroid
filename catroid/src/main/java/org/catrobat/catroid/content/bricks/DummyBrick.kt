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

package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.DummyScript
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction

class DummyBrick : ScriptBrickBaseType {

    private val serialVersionUID = 1L

    private var script: DummyScript;

    constructor() : this(DummyScript()) {
    }

    constructor(script: DummyScript) : super() {
        script.scriptBrick = this
        this.script = script
        script.isCommentedOut = true
        commentedOut = true;
    }

    override fun clone(): Brick {
        val clone = super.clone() as DummyBrick
        clone.script = script.clone() as DummyScript
        clone.script.scriptBrick = clone
        return clone
    }

    override fun addActionToSequence(sprite: Sprite?, sequence: ScriptSequenceAction?) {
    }

    override fun getViewResource(): Int {
        return R.layout.brick_dummy
    }

    override fun getScript(): Script {
        return script;
    }
}