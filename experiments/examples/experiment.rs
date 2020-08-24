// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

use anyhow::Result;
use experiments::{AppContext, Experiments};
fn main() -> Result<()> {
    let exp = Experiments::new(AppContext::default(), "../target/mydb", None).unwrap();
    exp.get_active_experiments();
    Ok(())
}
