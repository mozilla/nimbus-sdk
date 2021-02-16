/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.experiments.nimbus

import android.app.Application
import android.content.Context
import org.json.JSONObject

// This object is a pretend Android Components Nimbus. It exposes enough API to demonstrate
// configuring application features in `FakeFeatures`.

class Nimbus(
    private val context: Context = Application(),
    private val nimbusClient: NimbusClientInterface = NimbusClient(0)
) {
    // This is cute, but I don't know if we'll get to being able to do this.
    private val parseJsonInKotlin: Boolean = nimbusClient.getExperimentBranch("features-api") == "json"

    // Two new methods to get a feature configuration from Rust.
    // This method can be used even if we don't have code generation.
    fun getFeatureVariables(featureId: String): Variables =
        if (parseJsonInKotlin) {
            nimbusClient.getFeatureVariablesJson(featureId)?.let { JSONVariables(context, JSONObject(it)) }
                ?: NullVariables(context)
        } else {
            FFIVariables(context, nimbusClient, featureId)
        }

    // This version becomes useful when we're generating feature specific wrappers.
    // e.g. `FakeGeneratedFeatureConfig`.
    fun <T> getFeatureVariables(featureId: String, transform: (Variables) -> T): T =
        getFeatureVariables(featureId).let(transform)
}
