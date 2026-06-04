package cinemaapp.util;

import cinemaapp.model.Booking;
import cinemaapp.model.BookingItem;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates a ticket file for a confirmed booking.
 *
 * Saved to:  src/cinemaapp/util/TicketPDFGenerator.java
 *
 * Why it wasn't showing in NetBeans:
 *   The file was likely saved outside the src/cinemaapp/util/ folder,
 *   or the package declaration didn't match the folder it was in.
 *   NetBeans only shows .java files inside the source root that match
 *   their declared package.  This file must be physically at:
 *     src/cinemaapp/util/TicketPDFGenerator.java
 *   and declare:
 *     package cinemaapp.util;
 *
 * Output: an HTML file that opens in the default browser.
 * The user can then use File → Print → Save as PDF in the browser
 * to produce a proper PDF — no external library required.
 *
 * To upgrade to a real PDF later, add itextpdf-5.5.13.jar to your
 * NetBeans Libraries and use the commented method at the bottom.
 */
public class TicketPDFGenerator {

    /**
     * Generates and opens the ticket.
     *
     * @param booking       the confirmed Booking
     * @param movieTitle    resolved movie title
     * @param showtimeInfo  formatted showtime string (date + screen)
     * @param outputPath    full file path chosen by the user
     */
    public void generate(Booking booking,
                         String movieTitle,
                         String showtimeInfo,
                         String outputPath) throws IOException {

        // Ensure we always save as .html so the browser can open it
        String htmlPath = outputPath.endsWith(".html")
                ? outputPath
                : outputPath.replace(".pdf", ".html") ;

        if (!htmlPath.endsWith(".html")) htmlPath += ".html";

        String html = buildHTML(booking, movieTitle, showtimeInfo);
        Files.writeString(Path.of(htmlPath), html);

        // Open in the system default browser
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new File(htmlPath).toURI());
            }
        } catch (Exception e) {
            // Non-fatal — file is saved even if browser doesn't open
            System.err.println("Could not open browser: " + e.getMessage());
        }
    }

    // ── HTML ticket template ──────────────────────────────────────────────────

    private String buildHTML(Booking booking, String movieTitle, String showtimeInfo) {

        StringBuilder rows = new StringBuilder();
        for (BookingItem item : booking.getBookingItems()) {
            rows.append("<tr>")
                .append("<td>").append(escHtml(item.getSeatId())).append("</td>")
                .append("<td>").append(item.getAttendeeType()).append("</td>")
                .append("<td style='text-align:right'>$")
                .append(String.format("%.2f", item.getItemPrice())).append("</td>")
                .append("</tr>\n");
        }

        return "<!DOCTYPE html>\n<html lang='en'>\n<head>\n"
            + "<meta charset='UTF-8'>\n"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>\n"
            + "<title>AUT Cinema Ticket — " + escHtml(booking.getBookingCode()) + "</title>\n"
            + "<style>\n"
            + "  * { box-sizing: border-box; margin: 0; padding: 0; }\n"
            + "  body { font-family: Arial, sans-serif; background: #f4f4f8;"
            + "         display: flex; justify-content: center; padding: 40px 16px; }\n"
            + "  .ticket { background: white; border-radius: 12px; max-width: 580px;"
            + "            width: 100%; box-shadow: 0 4px 20px rgba(0,0,0,0.12);"
            + "            overflow: hidden; }\n"
            + "  .header { background: #1c1c2e; color: white; padding: 28px 32px; }\n"
            + "  .header h1 { font-size: 22px; margin-bottom: 4px; }\n"
            + "  .header p  { font-size: 13px; color: #aaa; }\n"
            + "  .code { font-size: 30px; font-weight: bold; letter-spacing: 4px;"
            + "          color: #e94560; margin: 16px 0 4px; }\n"
            + "  .body { padding: 28px 32px; }\n"
            + "  .row  { display: flex; gap: 40px; margin-bottom: 16px; }\n"
            + "  .field label { font-size: 11px; text-transform: uppercase;"
            + "                 color: #888; display: block; margin-bottom: 3px; }\n"
            + "  .field span  { font-size: 15px; font-weight: bold; color: #1c1c2e; }\n"
            + "  table { width: 100%; border-collapse: collapse; margin-top: 8px; }\n"
            + "  th { background: #1c1c2e; color: white; padding: 9px 12px;"
            + "       text-align: left; font-size: 13px; }\n"
            + "  td { padding: 9px 12px; border-bottom: 1px solid #eee;"
            + "       font-size: 13px; }\n"
            + "  tr:last-child td { border-bottom: none; }\n"
            + "  .total { font-size: 20px; font-weight: bold; color: #1c1c2e;"
            + "           text-align: right; margin-top: 16px; }\n"
            + "  .footer { background: #f9f9fb; padding: 18px 32px;"
            + "            text-align: center; color: #999; font-size: 12px;"
            + "            border-top: 1px solid #eee; }\n"
            + "  @media print { body { background: white; padding: 0; }"
            + "    .ticket { box-shadow: none; } }\n"
            + "</style>\n</head>\n<body>\n"
            + "<div class='ticket'>\n"
            + "  <div class='header'>\n"
            + "    <h1>🎬 AUT Cinema — Booking Ticket</h1>\n"
            + "    <p>Please present this at the door</p>\n"
            + "  </div>\n"
            + "  <div class='body'>\n"
            + "    <div class='code'>" + escHtml(booking.getBookingCode()) + "</div>\n"
            + "    <div class='row'>\n"
            + "      <div class='field'><label>Movie</label>"
            + "<span>" + escHtml(movieTitle) + "</span></div>\n"
            + "      <div class='field'><label>Showtime</label>"
            + "<span>" + escHtml(showtimeInfo) + "</span></div>\n"
            + "    </div>\n"
            + "    <table>\n"
            + "      <tr><th>Seat</th><th>Attendee Type</th><th style='text-align:right'>Price</th></tr>\n"
            + rows
            + "    </table>\n"
            + "    <div class='total'>Total Paid: $"
            + String.format("%.2f", booking.getTotalPrice()) + "</div>\n"
            + "  </div>\n"
            + "  <div class='footer'>Thank you for booking with AUT Cinema!</div>\n"
            + "</div>\n</body>\n</html>";
    }

    /** Escape special HTML characters to prevent broken output. */
    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}