// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

use anyhow::Result;
use experiments::{AppContext, Config, Experiments};
fn main() -> Result<()> {
    viaduct_reqwest::use_reqwest_backend();
    let config = Config {
        _server_url: Some("https://settings.stage.mozaws.net".to_string()),
        _bucket_name: Some("main".to_string()),
        _collection_name: Some("messaging-experiments".to_string()),
        uuid: None,
    };
    let exp = Experiments::new(AppContext::default(), "../target/mydb", Some(config)).unwrap();
    exp.get_experiments().iter().for_each(|exp| {
        println!("Experiment: {}", exp.id);
    });
    Ok(())
}
