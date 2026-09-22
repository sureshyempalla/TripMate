import SwiftUI
import shared

/// SwiftUI counterpart of androidApp's OfflineBanner — same message, same
/// muted (non-error) treatment, since offline is a fully-supported state
/// in this app, not a failure.
struct OfflineBannerView: View {
    let syncStatus: SyncStatus

    var body: some View {
        if syncStatus == .offline {
            Text("You're offline — showing your saved itinerary. Changes sync when you're back.")
                .font(.footnote.weight(.medium))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .padding(.horizontal, 16)
                .background(Color("DeepTeal"))
                .transition(.opacity)
        }
    }
}
