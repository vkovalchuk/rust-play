
#[allow(unused_assignments)]
pub fn ts() -> String {
    use std::time::SystemTime;
    static mut sys_time: Option<SystemTime> = None;

    use std::fmt::Write;
    let mut secs: u128 = 0;

    unsafe {
    if sys_time.is_none() {
        sys_time = Some(SystemTime::now());
    }
    secs = sys_time.unwrap().elapsed().unwrap().as_millis();
    }
    let mut result = String::new();
    write!(&mut result, "{:06}", secs);
        // "{:4}-{:02}-{:02} {:02}:{:02}:{:02}", t.year(), t.month(), t.day(), t.hour(), t.minute(), t.seconds());
    result
}
