package com.recetea.infrastructure.reports.openpdf;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.out.report.IStatsReportPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class OpenPdfStatsAdapter implements IStatsReportPort {

    private static final int FLUSH_EVERY = 50;

    private static final Color ACCENT     = new Color(0, 121, 107);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);

    private static final Font FONT_TITLE   = new Font(Font.HELVETICA, 18, Font.BOLD,   Color.DARK_GRAY);
    private static final Font FONT_SECTION = new Font(Font.HELVETICA, 12, Font.BOLD,   Color.DARK_GRAY);
    private static final Font FONT_HEADER  = new Font(Font.HELVETICA, 10, Font.BOLD,   Color.WHITE);
    private static final Font FONT_BODY    = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font FONT_SMALL   = new Font(Font.HELVETICA,  9, Font.ITALIC, Color.GRAY);
    private static final Font FONT_TOTAL   = new Font(Font.HELVETICA, 11, Font.BOLD,   new Color(0, 121, 107));

    @Override
    public void generateGlobalInventoryReport(Iterable<RecipeSummaryResponse> summaries, OutputStream outputStream) {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 50);
        try {
            PdfWriter.getInstance(doc, outputStream);
            doc.open();

            addReportTitle(doc);

            // Streaming table: rows are flushed to the OutputStream every FLUSH_EVERY records
            // to avoid loading the entire dataset into heap at once.
            PdfPTable table = buildCatalogueTableHeader();
            table.setComplete(false);
            doc.add(table);

            int total     = 0;
            int batchRows = 0;
            BigDecimal scoreSum = BigDecimal.ZERO;
            boolean alt = false;

            for (RecipeSummaryResponse r : summaries) {
                total++;
                if (r.averageScore() != null) scoreSum = scoreSum.add(r.averageScore());

                Color rowBg = alt ? LIGHT_GRAY : Color.WHITE;
                String avg  = r.averageScore() != null
                        ? r.averageScore().setScale(1, RoundingMode.HALF_UP).toPlainString()
                        : "—";

                table.addCell(bodyCell(r.title(),                           rowBg, Element.ALIGN_LEFT));
                table.addCell(bodyCell(r.categoryName(),                    rowBg, Element.ALIGN_LEFT));
                table.addCell(bodyCell(r.difficultyName(),                  rowBg, Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(r.prepTimeMinutes()), rowBg, Element.ALIGN_CENTER));
                table.addCell(bodyCell(avg,                                 rowBg, Element.ALIGN_CENTER));
                table.addCell(bodyCell(String.valueOf(r.totalRatings()),    rowBg, Element.ALIGN_CENTER));
                alt = !alt;
                batchRows++;

                if (batchRows == FLUSH_EVERY) {
                    doc.add(table);
                    batchRows = 0;
                }
            }

            table.setComplete(true);
            doc.add(table);

            addAggregateSection(doc, total, scoreSum);

        } catch (DocumentException e) {
            throw new InfrastructureException("Error al generar el informe de inventario global PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    // ── Sections ──────────────────────────────────────────────────────────────

    private void addReportTitle(Document doc) throws DocumentException {
        Paragraph title = new Paragraph("Mis Recetas Favoritas", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph subtitle = new Paragraph("Selección personal con métricas sociales", FONT_SMALL);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(18);
        doc.add(subtitle);
    }

    private PdfPTable buildCatalogueTableHeader() throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5f, 3f, 3f, 2f, 2f, 2f});
        table.setSpacingAfter(16);
        addAccentHeader(table, "Título", "Categoría", "Dificultad", "Tiempo (min)", "Puntuación", "Votos");
        return table;
    }

    private void addAggregateSection(Document doc, int total, BigDecimal scoreSum)
            throws DocumentException {

        BigDecimal globalAvg = scoreSum.divide(
                total == 0 ? BigDecimal.ONE : BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        Paragraph section = new Paragraph("Resumen Agregado", FONT_SECTION);
        section.setSpacingBefore(4);
        section.setSpacingAfter(6);
        doc.add(section);

        PdfPTable agg = new PdfPTable(2);
        agg.setWidthPercentage(40);
        agg.setHorizontalAlignment(Element.ALIGN_LEFT);
        agg.setWidths(new float[]{5f, 3f});

        addSummaryRow(agg, "Total de recetas",        String.valueOf(total));
        addSummaryRow(agg, "Puntuación media global", globalAvg.toPlainString());
        doc.add(agg);
    }

    // ── Cell / row helpers ────────────────────────────────────────────────────

    private void addAccentHeader(PdfPTable table, String... labels) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, FONT_HEADER));
            cell.setBackgroundColor(ACCENT);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderWidth(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private PdfPCell bodyCell(String text, Color background, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setBackgroundColor(background);
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_SECTION));
        labelCell.setBackgroundColor(LIGHT_GRAY);
        labelCell.setPadding(5);
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_TOTAL));
        valueCell.setPadding(5);
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
}
