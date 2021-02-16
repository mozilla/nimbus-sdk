package org.mozilla.experiments.nimbus

import android.graphics.Color

// FAKE GENERATED CODE (not yet)
// This shows two different models of accessing feature properties in a type safe manner.
// They differ in when they do the type coercion.
// However, they also show us a way for not passing JSON over the FFI, but still have a nice API.

// This example does all its work in a secondary constructor, then discards to the `variables` object.
// This has this has the benefit of being easy to use in a testing environment.
// If we know that every key is going to be used, this is the version to use.
// Because it has multiple constructors, this one (the data-class) will be better for re-use.
data class ButtonFeatureConfig1(
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
class ButtonFeatureConfig2(
    private val variables: Variables
) {
    val buttonColor: Int by lazy { variables.getColor("buttonColor") ?: Color.BLUE }
    val buttonText: String by lazy { variables.getText("buttonText") ?: "Okay then" }
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
