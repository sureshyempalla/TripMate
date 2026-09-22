import SwiftUI
import shared

/// SwiftUI counterpart of androidApp's NextUpBanner composable. Same
/// content, same terracotta accent — the two platforms should feel like
/// the same app, not two different products that happen to share a name.
struct NextUpBannerView: View {
    let banner: NextActivityBanner

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "play.fill")
                .foregroundStyle(.white)
            VStack(alignment: .leading, spacing: 2) {
                Text("NEXT UP")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.85))
                Text(banner.activityTitle)
                    .font(.headline)
                    .foregroundStyle(.white)
                if let place = banner.placeName {
                    Text(place)
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
            Spacer()
        }
        .padding(16)
        .background(Color("Terracotta"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
        .padding(.top, 8)
    }
}
