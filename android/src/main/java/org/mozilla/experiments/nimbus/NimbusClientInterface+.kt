/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.experiments.nimbus

var exampleJson: String? = null

// Dummy implementations of new Rust methods exposed via the FFI.
fun NimbusClientInterface.getFeatureVariablesJson(featureId: String) = exampleJson

fun NimbusClientInterface.getFeatureVariableString(featureId: String, key: String): String? =
    "dummy"
fun NimbusClientInterface.getFeatureVariableInt(featureId: String, key: String): Int? =
    0
fun NimbusClientInterface.getFeatureVariableBoolean(featureId: String, key: String): Boolean? =
    false
fun NimbusClientInterface.getFeatureVariableJsonString(featureId: String, key: String): String? =
    "{}"
