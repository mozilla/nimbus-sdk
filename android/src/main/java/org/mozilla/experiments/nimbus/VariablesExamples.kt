/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.experiments.nimbus

import android.app.AlertDialog
import android.app.Application
import android.graphics.Color
import android.widget.Button
import org.json.JSONObject
import uniffi.nimbus.BuildConfig

// We don't have a context or a nimbus right now, but need one for this to compile and run
val context = Application()
val nimbus = NimbusClient(0L)

var exampleJson: String? = null

// Some dummy methods to simulate where we may end up with either nimbus
// Due to our need to have a context, this will likely end up in Nimbus in Android Components.
fun NimbusClient.getFeatureVariablesJson(featureId: String) = exampleJson

fun NimbusClient.getFeatureVariables(featureId: String): Variables =
    getFeatureVariablesJson(featureId)?.let { Variables(context, JSONObject(it)) }
        ?: Variables(context)

fun <T> NimbusClient.getFeatureVariables(featureId: String, transform: (Variables) -> T): T =
    getFeatureVariables(featureId).let(transform)


////////////////////////////////////////////////////////////////////////////////////////////////
// Two examples of how we could generate the code.
// We'll do a simple feature and a complicated feature, with two different ways of generating
// the code: eager and lazy
////////////////////////////////////////////////////////////////////////////////////////////////
// This one is configuring feature1.
fun configureFeature1() {
    exampleJson = """
        {
            "buttonColor": "red",
            "buttonText": "treatment_button_label"
        }
    """.trimIndent()

    val config = nimbus.getFeatureVariables("example1") { EagerConfig1(it) }
    val lazyConfig = nimbus.getFeatureVariables("example1") { LazyConfig1(it) }

    val button = Button(context).apply {
        text = config.buttonText
        setBackgroundColor(config.buttonColor)
    }

    if (BuildConfig.DEBUG && config.buttonText != lazyConfig.buttonText) {
        error("Assertion failed")
    }

    if (BuildConfig.DEBUG && config.buttonColor != lazyConfig.buttonColor) {
        error("Assertion failed")
    }
}

// This example does all its work in a secondary constructor, then discards to the `variables` object.
// This has this has the benefit of being easy to use in a testing environment.
// If we know that every key is going to be used, this is the version to use.
// Because it has multiple constructors, this one (the data-class) will be better for re-use.
data class EagerConfig1(
    val buttonColor: Int,
    val buttonText: String
) {
    constructor(variables: Variables) : this(
        variables.getColor("buttonColor") ?: Color.BLUE,
        variables.getText("buttonText") ?: "Okay then"
    )
}

// This example does its work bit by bit, lazily grabbing the values out of JSON as it goes.
// If we think that not every key is going to be used, then this will be the less CPU intensive.
class LazyConfig1(
    private val variables: Variables
) {
    val buttonColor: Int by lazy { variables.getColor("buttonColor") ?: Color.BLUE }
    val buttonText: String by lazy { variables.getText("buttonText") ?: "Okay then" }
}

////////////////////////////////////////////////////////////////////////////////////////////////
// This one is configuring feature2. This will include a subset of variables:
// for a button. In reality we'll not be doing anything this complex with something so simple.
fun configureFeature2() {
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

    val config = nimbus.getFeatureVariables("feature2") { SelfDestructDialogConfig(it) }

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

// The eager method lends itself to re-use
data class ButtonConfig(
    val text: String,
    val color: Int
) {
    constructor(variables: Variables): this(
        text = variables.getText("text") ?: "Ok then",
        color = variables.getColor("color") ?: Color.BLUE
    )
}

data class SelfDestructDialogConfig(
    val title: String,
    val description: String,
    val icon: Int,
    val positiveButton: ButtonConfig,
    val negativeButton: ButtonConfig
) {
    constructor(variables: Variables): this(
        title = variables.getText("title") ?: "Auto destruct",
        icon = variables.getDrawableResource("icon") ?: /* R.drawable.ic_auto_destruct, */
                variables.asDrawableResource("ic_auto_destruct")!!, // This is to make it compile in this proposal.
        description = variables.getText("description") ?: /* variables.asText(R.string.auto_destruct_description), */
                variables.asText("auto_destruct_description"), // This is to make it compile in this proposal.
        positiveButton = variables.getVariables("positiveButton") { ButtonConfig(it) } ?: ButtonConfig("Ok then", Color.BLUE),
        negativeButton = variables.getVariables("negativeButton") { ButtonConfig(it) } ?: ButtonConfig("No", Color.RED)
    )
}

// We can do a lot with type safety and compile time checking to make this default generation
// cost free at runtime.

/*
title = variables.getText("title") ?: "Auto destruct",
icon = variables.getDrawableResource("icon") ?: R.drawable.ic_auto_destruct,
description = variables.getText("description") ?: variables.asText(R.string.auto_destruct_description),
positiveButton = variables.getVariables("positiveButton") { ButtonConfig(it) } ?:
    ButtonConfig(
        context.getString(R.string.auto_destruct_proceed),
        context.getColor(R.color.positive_button_color)
   ),
negativeButton = variables.getVariables("negativeButton") { ButtonConfig(it) } ?:
    ButtonConfig(
        context.getString(R.string.auto_destruct_cancel),
        Color.RED
   ),
*/
