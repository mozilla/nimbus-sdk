// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

pub mod error;
mod evaluator;
pub use error::*;
mod config;
mod http_client;
mod matcher;
mod persistence;
mod uuid;

use ::uuid::Uuid;
use anyhow::{anyhow, Result};
use chrono::{DateTime, Utc};
pub use config::Config;
use http_client::SettingsClient;
pub use matcher::AppContext;
use serde_derive::*;
use std::path::Path;

const DEFAULT_TOTAL_BUCKETS: u32 = 10000;

/// Experiments is the main struct representing the experiements state
/// It should hold all the information needed to communcate a specific user's
/// Experiementation status
#[derive(Debug)]
pub struct Experiments {
    experiments: Vec<Experiment>,
    app_context: AppContext,
    uuid: Uuid,
    client: http_client::Client,
}

impl Experiments {
    pub fn new<P: AsRef<Path>>(
        app_context: AppContext,
        _db_path: P,
        config: Option<Config>,
    ) -> Result<Self> {
        let client = http_client::Client::new(config.clone())?;
        let experiments = client.get_experiments()?;
        let uuid = uuid::generate_uuid(config);
        Ok(Self {
            experiments,
            app_context,
            uuid,
            client,
        })
    }

    pub fn get_experiment_branch(&self) -> Result<String> {
        Err(anyhow!("Not implemented"))
    }

    pub fn get_experiments(&self) -> &Vec<Experiment> {
        &self.experiments
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct Experiment {
    pub id: String,
    pub filter_expression: String,
    pub targeting: Option<String>,
    pub enabled: bool,
    pub arguments: ExperimentArguments,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ExperimentArguments {
    pub slug: String,
    pub user_facing_name: String,
    pub user_facing_description: Option<String>,
    pub active: Option<bool>,
    pub is_enrollment_paused: bool,
    pub bucket_config: Option<BucketConfig>,
    pub features: Option<Vec<String>>,
    pub branches: Vec<Branch>,
    pub end_date: Option<DateTime<Utc>>,
    pub proposed_duration: Option<u32>,
    pub proposed_enrollment: Option<u32>,
    pub reference_branch: Option<String>,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct Branch {
    pub slug: String,
    pub ratio: u32,
    pub group: Option<Vec<Group>>,
    pub value: Option<BranchValue>,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum Group {
    Cfr,
    AboutWelcome,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct BranchValue {} // TODO: This is not defined explicitly in the nimbus schema yet

fn default_buckets() -> u32 {
    DEFAULT_TOTAL_BUCKETS
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct BucketConfig {
    pub randomization_unit: RandomizationUnit,
    pub namespace: String,
    pub start: u32,
    pub count: u32,
    #[serde(default = "default_buckets")]
    pub total: u32,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "snake_case")]
pub enum RandomizationUnit {
    ClientId,
    NormandyId,
    #[serde(rename = "userId")]
    UserId,
}
