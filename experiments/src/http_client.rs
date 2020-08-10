/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

//! This is a simple Http client that uses viaduct to retrieve experiment data from the server
//! Currently configured to use Kinto and the old schema, although that would change once we start
//! Working on the real Nimbus schema.

use super::Experiment;
use crate::error::{Error, Result};
use crate::Config;
use serde_derive::*;
use url::Url;
use viaduct::{status_codes, Request, Response};

const DEFAULT_BASE_URL: &str = "https://settings.stage.mozaws.net"; // TODO: Replace this with prod
const DEFAULT_BUCKET_NAME: &str = "main";
const DEFAULT_COLLECTION_NAME: &str = "messaging-experiment";

// Making this a trait so that we can mock those later.
pub(crate) trait SettingsClient {
    fn get_experiements_metadata(&self) -> Result<String>;
    fn get_experiments(&self) -> Result<Vec<Experiment>>;
}

#[derive(Debug)]
pub struct Client {
    base_url: Url,
    bucket_name: String,
    collection_name: String,
}

#[derive(Deserialize)]
struct ExperimentResponse {
    data: Vec<Experiment>,
}

impl Client {
    #[allow(unused)]
    pub fn new(config: Option<Config>) -> Result<Self> {
        let (base_url, bucket_name, collection_name) = Self::get_params_from_config(config)?;
        Ok(Self {
            base_url,
            bucket_name,
            collection_name,
        })
    }

    fn get_params_from_config(config: Option<Config>) -> Result<(Url, String, String)> {
        Ok(match config {
            Some(config) => {
                let base_url = config._server_url.unwrap_or(DEFAULT_BASE_URL.to_string());
                let bucket_name = config
                    ._bucket_name
                    .unwrap_or(DEFAULT_BUCKET_NAME.to_string());
                let collection_name = config
                    ._collection_name
                    .unwrap_or(DEFAULT_COLLECTION_NAME.to_string());
                (Url::parse(&base_url)?, bucket_name, collection_name)
            }
            None => (
                Url::parse(DEFAULT_BASE_URL)?,
                DEFAULT_BUCKET_NAME.to_string(),
                DEFAULT_COLLECTION_NAME.to_string(),
            ),
        })
    }

    fn make_request(&self, request: Request) -> Result<Response> {
        let resp = request.send()?;
        if resp.is_success() || resp.status == status_codes::NOT_MODIFIED {
            Ok(resp)
        } else {
            Err(Error::ResponseError(resp.text().to_string()))
        }
    }
}

impl SettingsClient for Client {
    fn get_experiements_metadata(&self) -> Result<String> {
        unimplemented!();
    }

    fn get_experiments(&self) -> Result<Vec<Experiment>> {
        let path = format!(
            "buckets/{}/collections/{}/records",
            &self.bucket_name, &self.collection_name
        );
        let url = self.base_url.join(&path)?;
        let req = Request::get(url);
        let resp = self.make_request(req)?.json::<ExperimentResponse>()?;
        Ok(resp.data)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{Branch, BranchValue, BucketConfig, ExperimentArguments, Group, RandomizationUnit};
    use mockito::mock;

    #[test]
    fn test_get_experiments_from_schema() {
        viaduct_reqwest::use_reqwest_backend();
        let body = r#"
        {
            "data":  [
                {
                    "id": "ABOUTWELCOME-PULL-FACTOR-REINFORCEMENT-76-RELEASE",
                    "enabled": true,
                    "filter_expression": "(env.version >= '76.' && env.version < '77.' && env.channel == 'release' && !(env.telemetry.main.environment.profile.creationDate < ('2020-05-13'|date / 1000 / 60 / 60 / 24))) || (locale == 'en-US' && [userId, \"aboutwelcome-pull-factor-reinforcement-76-release\"]|bucketSample(0, 2000, 10000) && (!('trailhead.firstrun.didSeeAboutWelcome'|preferenceValue) || 'bug-1637316-message-aboutwelcome-pull-factor-reinforcement-76-rel-release-76-77' in activeExperiments))",
                    "arguments": {
                        "slug": "bug-1637316-message-aboutwelcome-pull-factor-reinforcement-76-rel-release-76-77",
                        "userFacingName": "About:Welcome Pull Factor Reinforcement",
                        "userFacingDescription": "4 branch experiment different variants of about:welcome with a goal of testing new experiment framework and get insights on whether reinforcing pull-factors improves retention. Test deployment of multiple branches using new experiment framework",
                        "isEnrollmentPaused": true,
                        "active": true,
                        "bucketConfig": {
                            "randomizationUnit": "normandy_id",
                            "namespace": "bug-1637316-message-aboutwelcome-pull-factor-reinforcement-76-rel-release-76-77",
                            "start": 0,
                            "count": 2000,
                            "total": 10000
                        },
                        "startDate": "2020-06-17T23:20:47.230Z",
                        "endDate": null,
                        "proposedDuration": 28,
                        "proposedEnrollment": 7,
                        "referenceBranch": "control",
                        "features": [],
                        "branches": [
                            { "slug": "control", "ratio": 1, "value": {}, "group": ["cfr"] },
                            { "slug": "treatment-variation-b", "ratio": 1, "value": {} }
                        ]
                    }
                }
            ]
        }"#;
        let m = mock(
            "GET",
            "/buckets/main/collections/messaging-experiment/records",
        )
        .with_body(body)
        .with_status(200)
        .with_header("content-type", "application/json")
        .create();
        let config = Config {
            _server_url: Some(mockito::server_url().to_string()),
            _collection_name: None,
            _bucket_name: None,
            uuid: None,
        };
        let http_client = Client::new(Some(config)).unwrap();
        let resp = http_client.get_experiments().unwrap();
        m.expect(1).assert();
        assert_eq!(resp.len(), 1);
        let exp = &resp[0];
        assert_eq!(exp.clone(), Experiment {
            id: "ABOUTWELCOME-PULL-FACTOR-REINFORCEMENT-76-RELEASE".to_string(),
            enabled: true,
            filter_expression: "(env.version >= '76.' && env.version < '77.' && env.channel == 'release' && !(env.telemetry.main.environment.profile.creationDate < ('2020-05-13'|date / 1000 / 60 / 60 / 24))) || (locale == 'en-US' && [userId, \"aboutwelcome-pull-factor-reinforcement-76-release\"]|bucketSample(0, 2000, 10000) && (!('trailhead.firstrun.didSeeAboutWelcome'|preferenceValue) || 'bug-1637316-message-aboutwelcome-pull-factor-reinforcement-76-rel-release-76-77' in activeExperiments))".to_string(),
            arguments: ExperimentArguments {
                    slug: "bug-1637316-message-aboutwelcome-pull-factor-reinforcement-76-rel-release-76-77".to_string(),
                    user_facing_name: "About:Welcome Pull Factor Reinforcement".to_string(),
                    user_facing_description: Some("4 branch experiment different variants of about:welcome with a goal of testing new experiment framework and get insights on whether reinforcing pull-factors improves retention. Test deployment of multiple branches using new experiment framework".to_string()),
                    is_enrollment_paused: true,
                    active: Some(true),
                    bucket_config: Some(BucketConfig {
                        randomization_unit: RandomizationUnit::NormandyId,
                        namespace: "bug-1637316-message-aboutwelcome-pull-factor-reinforcement-76-rel-release-76-77".to_string(),
                        start: 0,
                        count: 2000,
                        total: 10000
                    }),
                    end_date: None,
                    proposed_duration: Some(28),
                    proposed_enrollment: Some(7),
                    reference_branch: Some("control".to_string()),
                    features: Some(vec![]),
                    branches: vec![
                        Branch { slug: "control".to_string(), ratio: 1, value: Some(BranchValue {}), group: Some(vec![Group::Cfr]) },
                        Branch { slug: "treatment-variation-b".to_string(), ratio: 1, value: Some(BranchValue {}), group: None }
                    ]
                },
            targeting: None,
        })
    }
}
