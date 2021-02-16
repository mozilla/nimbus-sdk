/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.experiments.nimbus

import android.app.AlertDialog
import android.app.Application
import android.graphics.Color
import android.widget.Button

// This file shows some features being configured with variables coming from Nimbus.
// This code is an example of what might be written by application developers.

val context = Application()
val nimbus = Nimbus()

////////////////////////////////////////////////////////////////////////////////////////////////
// Configuring a simple button.
//
// In reality we'll not be doing anything this complex with something so simple.
////////////////////////////////////////////////////////////////////////////////////////////////

fun configureButtonWithoutCodeGen() {
    exampleJson = """
        {
            "buttonColor": "red",
            "buttonText": "treatment_button_label"
        }
    """.trimIndent()

    // config and lazyConfig are ostensibly the same shape, perform the JSON look up
    // at different times.
    val config = nimbus.getFeatureVariables("button-feature")

    val button = Button(context).apply {
        text = config.getText("buttonText") ?: "Ok then"
        setBackgroundColor(config.getColor("buttonColor") ?: Color.BLUE)
    }
}

// We can treat the Variables object with a code generated wrapper, so we can give the app developer
// a degree of type safety and editor integration. The generated classes are in
// `FakeGeneratedFeatureConfig`.
fun configureButtonV1() {
    exampleJson = """
        {
            "buttonColor": "red",
            "buttonText": "treatment_button_label"
        }
    """.trimIndent()

    // config and lazyConfig are ostensibly the same shape, perform the JSON look up
    // at different times.
    val config = nimbus.getFeatureVariables("button-feature") { ButtonFeatureConfig1(it) }

    val button = Button(context).apply {
        text = config.buttonText
        setBackgroundColor(config.buttonColor)
    }
}

// This is a second example of the same feature. This time, the generated code is doing the look up
// lazily.
fun configureButtonV2() {
    exampleJson = """
        {
            "buttonColor": "red",
            "buttonText": "treatment_button_label"
        }
    """.trimIndent()

    val config = nimbus.getFeatureVariables("button-feature") { ButtonFeatureConfig2(it) }

    val button = Button(context).apply {
        text = config.buttonText
        setBackgroundColor(config.buttonColor)
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////
// The next feature is a little more complicated: it is a dialog and has two buttons. The buttons
// are configured using a child object each.
//
// In reality we'll not be doing anything this complex with something so simple.
////////////////////////////////////////////////////////////////////////////////////////////////
// This first time around, the developer manages the keys and defaults in code.
fun configureDialogWithoutCodeGeneration() {
    exampleJson = """
        {
            "title": "Self Destruct",
            "description": "Are you sure you wish to destroy the ship?",
            "positiveButton": {
                "color": "red",
                "text": "Continue"
            },
            "negativeButton": {
                "text": "No, cancel"
            }
        }
    """.trimIndent()

    val config = nimbus.getFeatureVariables("self-destruct-dialog")

    val dialogBuilder = AlertDialog.Builder(context).apply {
        setTitle(config.getText("title") ?: "Auto destruct")
        setMessage(config.getText("description") ?: "Continue?")
        setIcon(config.getDrawableResource("icon") ?: 0)

        val positiveButton = config.getVariables("positiveButton")
        val negativeButton = config.getVariables("negativeButton")
        setPositiveButton(positiveButton?.getText("text") ?: "Okay then") { _, _ -> /* on click */ }
        setNegativeButton(negativeButton?.getText("text") ?: "Cancel") { dialog, _ ->
            dialog.cancel()
        }
        create()
    }

    dialogBuilder.show()
}

// Finally, we configure the same dialog with the configuration wrapped in a generated typesafe
// wrapper, in `FakeGeneratedFeatureConfig`.
fun configureDialogWithCodeGeneration() {
    exampleJson = """
        {
            "title": "Self Destruct",
            "description": "Are you sure you wish to destroy the ship?",
            "positiveButton": {
                "color": "red",
                "text": "Continue"
            },
            "negativeButton": {
                "text": "No, cancel"
            }
        }
    """.trimIndent()

    val config = nimbus.getFeatureVariables("self-destruct-dialog") { SelfDestructDialogConfig(it) }

    val dialogBuilder = AlertDialog.Builder(context).apply {
        setTitle(config.title)
        setMessage(config.description)
        setIcon(config.icon)

        setPositiveButton(config.positiveButton.text) { _, _ -> /* on click */ }
        setNegativeButton(config.negativeButton.text) { dialog, _ ->
            dialog.cancel()
        }
        create()
    }

    dialogBuilder.show()
}
