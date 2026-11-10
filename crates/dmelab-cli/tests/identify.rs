use std::process::Command;

#[test]
fn absent_serial_device_reports_the_failed_operation_and_path() {
    let path = "/dmelab-nonexistent-device-test/serial";
    let output = Command::new(env!("CARGO_BIN_EXE_dmelab"))
        .args(["identify", "--port", path])
        .output()
        .expect("CLI should start");
    assert!(!output.status.success());
    assert!(output.stdout.is_empty());
    let stderr = String::from_utf8(output.stderr).unwrap();
    assert!(stderr.contains("Failed to open K-line adapter"));
    assert!(stderr.contains(path));
}
