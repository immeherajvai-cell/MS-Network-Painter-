# MS Network Printer — starter project

This is a lightweight Android starter project (Kotlin, min Android 8.0 / API 26) for selecting PDFs and images, capturing a camera photo, and sending printable content to Android's system Print dialog.

## Important compatibility note
Entering a printer IP in this app does **not** create a printer driver or prove that Toshiba e-STUDIO 452 accepts PDF/PCL on a raw socket. This first version uses Android Print Framework. Configure a compatible print service on the phone (for example, the printer manufacturer's supported service or a compatible IPP/Mopria service) and select the printer in the Android Print dialog.

Direct IP printing, printer status, and Toshiba-specific protocol support are intentionally not claimed until tested on the actual printer. The IP field is currently informational only.

## Build APK
1. Upload the project files to a GitHub repository.
2. Open **Actions** and run **Build Android APK** (or push to `main`).
3. Download the `MS-Network-Printer-debug-apk` artifact.
4. Install the debug APK on an Android device.

## Included
- PDF and common image selection (JPG/JPEG/PNG)
- Camera photo capture
- Android Print dialog integration
- GitHub Actions debug APK workflow

## Not yet included / needs testing
- True document scanning with crop/edge detection
- Direct IP socket printing and Toshiba protocol verification
- Real printer paper/jam/toner status
- Word/Excel conversion (requires a compatible rendering/conversion component)
