/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.experiments.nimbus

import android.content.Context
import android.graphics.Color
import org.json.JSONObject

/**
* It provides the type-coercion tooling and resource look-up to be immediately useful to feature
* developers.
*
* The feature developer requests a typed value with a specific `key`. If the key is present, and
* the value is of the correct type, then it is returned. If neither of these are true, then `null`
* is returned.
*
* ```
* val config = nimbus.getFeatureVariables("submitButton")
*
* submitButton.text = config.getText("submitButton.text") ?: R.string.submit_button_label
* submitButton.color = config.getColor("submitButton.color") ?: R.color.button_default
*
* ```
*
* This may become the basis of a generated-from-manifest solution.
*/
interface Variables {
    val context: Context
    // Lower level accessors that can come across the FFI.
    fun getString(key: String): String?
    fun getInt(key: String): Int?
    fun getBoolean(key: String): Boolean?

    // Get a child configuration object.
    fun getVariables(key: String): Variables?
    // This may be important when transforming in to a code generated object.
    fun <T> getVariables(key: String, transform: (Variables) -> T) = getVariables(key)?.let(transform)

    // Platform specific types, deserialized from the lower level types.
    fun getColor(key: String) = getString(key)?.let(this::asColor)
    fun getTextResource(key: String, context: Context = this.context): Int? = getString(key)?.let {
        context.getResource(it, "string")
    }
    fun getText(key: String, context: Context = this.context) = getString(key)?.let(this::asText)
    fun getDrawableResource(key: String, context: Context = this.context) = getString(key)?.let(this::asDrawableResource)

    // These `as*` methods become useful when transforming values found in JSON to actual values
    // the app will use. They're broken out here so they can be re-used by codegen generating
    // defaults from manifest information.
    fun asColor(string: String) = Color.parseColor(string)
    fun asText(res: Int) = context.getString(res)
    fun asText(string: String) = context.getResource(string, "string")?.let(this::asText)
        ?: string
    fun asDrawableResource(string: String) = context.getResource(string, "drawable")
}

// Get a resource Int if it exists from the context resources.
// Here we're using it for icons and strings.
// This will help us look after translations, dark mode, screen size, pixel density etc etc.
private fun Context.getResource(resName: String, defType: String): Int? {
    val res = resources.getIdentifier(resName, defType, packageName)
    return if (res != 0) {
        res
    } else {
        null
    }
}

/**
 * A thin wrapper around the JSON produced by the `get_feature_variables_json(feature_id)` call, useful
 * for configuring a feature, but without needing the developer to know about experiment specifics.
 */
class JSONVariables(
    override val context: Context,
    private val json: JSONObject = JSONObject()
) : Variables {
    // These `get*` methods get values from the wrapped JSON object, and transform them using the
    // `as*` methods.
    override fun getString(key: String) = json.value<String>(key)

    override fun getInt(key: String) = json.value<Int>(key)

    override fun getBoolean(key: String) = json.value<Boolean>(key)

    // Methods used to get sub-objects. We immediately re-wrap an JSON object if it exists.
    override fun getVariables(key: String) = json.value<JSONObject>(key)?.let { JSONVariables(context, it) }
}

// A typed getter. If the key is not present, or the value is JSONNull, or the wrong type
// returns `null`.
private inline fun <reified T> JSONObject.value(key: String): T? {
    if (!this.isNull(key)) {
        val value = this.get(key)
        if (value is T) {
            return value
        }
    }
    return null
}

// Another implementation of `Variables` may be grabbing values from across the FFI.
class FFIVariables(
    override val context: Context,
    private val nimbus: NimbusClientInterface,
    private val featureId: String
) : Variables {
    // These `get*` methods get values from the wrapped JSON object, and transform them using the
    // `as*` methods.
    override fun getString(key: String) = nimbus.getFeatureVariableString(featureId, key)

    override fun getInt(key: String) = nimbus.getFeatureVariableInt(featureId, key)

    override fun getBoolean(key: String) = nimbus.getFeatureVariableBoolean(featureId, key)

    // Methods used to get sub-objects. We immediately re-wrap an JSON object if it exists.
    override fun getVariables(key: String) = nimbus.getFeatureVariableJsonString(featureId, key)?.let { JSONVariables(context, JSONObject(it)) }
}

// Another implementation of `Variables` may just return null for everything.
class NullVariables(
    override val context: Context
) : Variables {
    override fun getString(key: String): String? = null

    override fun getInt(key: String): Int? = null

    override fun getBoolean(key: String): Boolean? = null

    override fun getVariables(key: String): Variables? = null
}
