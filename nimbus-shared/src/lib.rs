use schemars::JsonSchema;

pub const VERSION: &'static str = env!("CARGO_PKG_VERSION");

#[derive(JsonSchema)]
#[allow(non_camel_case_types)]
pub enum RandomizationUnit {
    nimbus_id,
    normandy_id,
}

#[derive(JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct NimbusExperiment {
    /// Version of the NimbusExperiment schema this experiment refers to
    pub schema_version: String,

    /// Unique identifier for the experiment
    pub slug: String,

    /// A specific product such as Firefox Desktop or Fenix that supports Nimbus experiments
    pub application: String,

    /// Public name of the experiment displayed on "about:studies"
    pub user_facing_name: String,

    /// Short public description of the experiment displayed on on "about:studies"
    pub user_facing_description: String,

    /// When this property is set to true, the the SDK should not enroll new users into the experiment that have not already been enrolled.
    pub is_enrollment_paused: bool,

    /// Bucketing configuration
    pub bucket_config: BucketConfig,

    /// A list of probe set slugs relevant to the experiment analysis
    pub probe_sets: Vec<String>,

    /// Branch configuration for the experiment
    pub branches: Vec<Branch>,

    /// JEXL expression used to filter experiments based on locale, geo, etc.
    pub targeting: Option<String>,

    /// Actual publish date of the experiment.
    /// Note that this value is expected to be null in Remote Settings.
    pub start_date: Option<String>,

    /// Actual end date of the experiment.
    /// Note that this value is expected to be null in Remote Settings.
    pub end_date: Option<String>,

    /// Duration of the experiment from the start date in days.
    /// Note that this property is only used during the analysis phase (not by the SDK)
    pub proposed_duration: Option<u32>,

    /// This represents the number of days that we expect to enroll new users.
    /// that this property is only used during the analysis phase (not by the SDK)
    pub proposed_enrollment: u32,

    /// The slug of the reference branch (that is, which branch we consider "control")
    pub reference_branch: Option<String>,

    /// This is NOT used by Nimbus, but has special functionality in Remote Settings.
    /// See https://remote-settings.readthedocs.io/en/latest/target-filters.html#how
    pub filter_expression: String,

    /// Unique identifier for the experiment. This is a duplicate of slug, but is a required field
    /// for all Remote Settings records.
    pub id: String,
}

#[derive(JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct FeatureConfig {
    /// The identifier for the feature config
    ///
    /// e.g. "aboutwelcome" is the identifier for feature
    pub feature_id: String,
    /// Should the code instrumented with the feature config be off or on?
    pub enabled: bool,
}

#[derive(JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct Branch {
    /// Identifier for the branch
    pub slug: String,
    /// Relative ratio of population for the branch (e.g. if branch A=1 and branch B=3,
    /// branch A would get 25% of the population)
    pub ratio: u32,
    pub feature: Option<FeatureConfig>,
}

#[derive(JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct BucketConfig {
    /// A unique, stable identifier for the user used as an input to bucket hashing
    pub randomization_unit: RandomizationUnit,

    /// Additional inputs to the hashing function
    pub namespace: String,

    /// Index of start of the range of buckets
    pub start: u32,

    /// Number of buckets to check
    pub count: u32,

    /// Total number of buckets. You can assume this will always be 10000.
    pub total: u32,
}
