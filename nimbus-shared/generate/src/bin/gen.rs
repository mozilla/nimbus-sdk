extern crate nimbus_shared;
#[macro_use]
extern crate schemars;
extern crate serde_json;

use std::env::current_dir;
use std::fs::write;
use std::fs::create_dir_all;
use std::path::PathBuf;
use schemars::schema::RootSchema;
use nimbus_shared::{NimbusExperiment, VERSION};

fn main() {
  let mut out_dir = current_dir().unwrap();
  out_dir.push("schemas");

  create_dir_all(&out_dir).unwrap();

  write_schema(&schema_for!(NimbusExperiment), &out_dir);
}

fn write_schema(schema: &RootSchema, out_dir: &PathBuf) {
  let title = schema
    .schema
    .metadata
    .as_ref()
    .map(|b| b.title.clone().unwrap_or_else(|| "untitled".to_string()))
    .unwrap_or_else(|| "unknown".to_string());

  let path = out_dir.join(format!("{}.{}.json", &title, VERSION));
  let json = serde_json::to_string_pretty(schema).unwrap();
  write(&path, json + "\n").unwrap();
  println!("Created {}", path.to_str().unwrap());
}
