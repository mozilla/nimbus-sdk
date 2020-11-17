/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

//! This is an approximation of the metrics code that Glean might generate
//! for us from our metrics.yaml file, once Glean's support for targetting
//! Rust code is more fully developed.
//!
//! For now, it's a nice little proof-of-concept of emitting Glean metrics
//! directly from Rust code.

use glean::{private::BooleanMetric, CommonMetricData, Lifetime};

#[allow(non_upper_case_globals)]
pub static rust_was_here: once_cell::sync::Lazy<BooleanMetric> =
    once_cell::sync::Lazy::new(|| {
        // The Glean Rust Language Bindings use an internal worker thread to
        // dispatch metrics in the background. This dispatcher will queue up
        // any tasks submitted before Glean is initialized by the application,
        // presumably so they don't operate on partial state.
        //
        // Unfortunately, that's a problem for our use-case. When our Rust
        // component is used in a consuming app, Glean gets initialized by
        // the application's Kotlin code, which uses its own separate dispatcher
        // mechanism. Nothing tells the Rust code that Glean has been initialized
        // externally, and the RLB dispatcher just keeps queueing up tasks and
        // never actually dispatching them.
        //
        // We'll need to figure out an approach here with the Glean team.
        // For now I'm lighty hacked up the Glean RLB code to expose this
        // function that we can call to just pretend initialization was done
        // and hope for the best...
        let _ = glean::dispatcher::flush_init();
        // This is the actual metric object.
        BooleanMetric::new(CommonMetricData {
            name: "rust_was_here".into(),
            category: "nimbus_sdk".into(),
            send_in_pings: vec!["metrics".into()],
            disabled: false,
            lifetime: Lifetime::Ping,
            ..Default::default()
        })
    });
