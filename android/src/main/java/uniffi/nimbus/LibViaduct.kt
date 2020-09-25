/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// This is a clone of LibViaduct.kt from application-services, but is in the
// `uniffi.nimbus` namespace for use from within nimbus. It is intended as a
// short term hack which we would out a better build and packaging strategy.
// It is copied as it expects to find the viaduct functions in libnimbus.so
// rather than the app-services "megazord"
package uniffi.nimbus;

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import mozilla.appservices.support.native.RustBuffer

@Suppress("FunctionNaming", "TooGenericExceptionThrown")
internal interface LibViaduct : Library {
    companion object {
        internal var INSTANCE: LibViaduct = {
            Native.load<LibViaduct>("nimbus", LibViaduct::class.java)
        }()
    }

    fun viaduct_destroy_bytebuffer(b: RustBuffer.ByValue)
    // Returns null buffer to indicate failure
    fun viaduct_alloc_bytebuffer(sz: Int): RustBuffer.ByValue
    // Returns 0 to indicate redundant init.
    fun viaduct_initialize(cb: RawFetchCallback): Byte

    fun viaduct_log_error(s: String)
}

internal interface RawFetchCallback : Callback {
    fun invoke(b: RustBuffer.ByValue): RustBuffer.ByValue
}
