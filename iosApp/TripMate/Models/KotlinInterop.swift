import Foundation
import shared

// Small formatting helpers so the Views stay declarative. kotlinx.datetime
// types (LocalDate) and epoch-millis Longs cross the Kotlin/Swift bridge as
// plain values, so these are ordinary Swift extensions, not wrappers.

extension Trip {
    func toKotlinDisplayString() -> String { startDate.toDisplayString() }
}

extension Kotlinx_datetimeLocalDate {
    func toDisplayString() -> String {
        "\(monthNumber)/\(dayOfMonth)/\(year)"
    }
}

extension Int64 {
    /// Renders an epoch-millis timestamp (as used by Activity.startAtEpochMillis)
    /// as a local "h:mm a" time, matching androidApp's equivalent formatter.
    func toTimeLabel() -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(self) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter.string(from: date)
    }
}
