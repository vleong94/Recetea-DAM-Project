package com.recetea.infrastructure.reports.openpdf;

import com.recetea.core.recipe.application.ports.out.report.IRecipeReportPort;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.List;
import org.openpdf.text.ListItem;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.math.RoundingMode;

public class OpenPdfRecipeAdapter implements IRecipeReportPort {

    private static final Color ACCENT     = new Color(63, 81, 181);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);

    private static final Font FONT_TITLE   = new Font(Font.HELVETICA, 20, Font.BOLD,  Color.DARK_GRAY);
    private static final Font FONT_SECTION = new Font(Font.HELVETICA, 12, Font.BOLD,  Color.DARK_GRAY);
    private static final Font FONT_HEADER  = new Font(Font.HELVETICA, 10, Font.BOLD,  Color.WHITE);
    private static final Font FONT_BODY    = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font FONT_SMALL   = new Font(Font.HELVETICA,  9, Font.ITALIC, Color.GRAY);

    @Override
    public void generateTechnicalSheet(Recipe recipe, OutputStream outputStream) {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        try {
            PdfWriter.getInstance(doc, outputStream);
            doc.open();

            addHeader(doc, recipe);
            addMetaTable(doc, recipe);
            addIngredientsTable(doc, recipe);
            addSteps(doc, recipe);
            addSocialFooter(doc, recipe);

        } catch (DocumentException e) {
            throw new InfrastructureException("Error al generar la ficha técnica PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    // ── Sections ──────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Recipe recipe) throws DocumentException {
        Paragraph title = new Paragraph(recipe.getTitle(), FONT_TITLE);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph author = new Paragraph("Autor: ID " + recipe.getAuthorId().value(), FONT_SMALL);
        author.setSpacingAfter(14);
        doc.add(author);
    }

    private void addMetaTable(Document doc, Recipe recipe) throws DocumentException {
        doc.add(sectionParagraph("Información General"));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 7f});
        table.setSpacingAfter(14);

        addMetaRow(table, "Categoría",             recipe.getCategory().getName());
        addMetaRow(table, "Dificultad",            recipe.getDifficulty().getName());
        addMetaRow(table, "Tiempo de preparación", recipe.getPreparationTimeMinutes().value() + " min");
        addMetaRow(table, "Raciones",              String.valueOf(recipe.getServings().value()));
        if (recipe.getDescription() != null && !recipe.getDescription().isBlank()) {
            addMetaRow(table, "Descripción",       recipe.getDescription());
        }
        doc.add(table);
    }

    private void addIngredientsTable(Document doc, Recipe recipe) throws DocumentException {
        doc.add(sectionParagraph("Ingredientes"));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2f, 6f});
        table.setSpacingAfter(14);

        addAccentHeader(table, "Cantidad", "Unidad", "Ingrediente");

        boolean alt = false;
        for (RecipeIngredient ing : recipe.getIngredients()) {
            Color rowBg = alt ? LIGHT_GRAY : Color.WHITE;
            String qty  = ing.getQuantity().stripTrailingZeros().toPlainString();
            String unit = ing.getUnitAbbreviation()  != null ? ing.getUnitAbbreviation()  : "";
            String name = ing.getIngredientName()     != null ? ing.getIngredientName()     : "";
            for (String text : new String[]{qty, unit, name}) {
                PdfPCell cell = bodyCell(text, rowBg);
                cell.setHorizontalAlignment(text.equals(qty) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                table.addCell(cell);
            }
            alt = !alt;
        }
        doc.add(table);
    }

    private void addSteps(Document doc, Recipe recipe) throws DocumentException {
        doc.add(sectionParagraph("Preparación"));

        List list = new List(List.ORDERED);
        list.setIndentationLeft(16);
        list.setSymbolIndent(10);
        for (RecipeStep step : recipe.getSteps()) {
            ListItem item = new ListItem(step.instruction(), FONT_BODY);
            item.setSpacingAfter(4);
            list.add(item);
        }
        doc.add(list);
    }

    private void addSocialFooter(Document doc, Recipe recipe) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(10);
        String avg = recipe.getAverageScore().setScale(1, RoundingMode.HALF_UP).toPlainString();
        footer.add(new Chunk("Puntuación media: ",      FONT_SECTION));
        footer.add(new Chunk(avg + " / 5     ",         FONT_BODY));
        footer.add(new Chunk("Valoraciones totales: ",  FONT_SECTION));
        footer.add(new Chunk(String.valueOf(recipe.getTotalRatings()), FONT_BODY));
        doc.add(footer);
    }

    // ── Cell / row helpers ────────────────────────────────────────────────────

    private Paragraph sectionParagraph(String text) {
        Paragraph p = new Paragraph(text, FONT_SECTION);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        return p;
    }

    private void addMetaRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_SECTION));
        labelCell.setBackgroundColor(LIGHT_GRAY);
        labelCell.setPadding(5);
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_BODY));
        valueCell.setPadding(5);
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(valueCell);
    }

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

    private PdfPCell bodyCell(String text, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setBackgroundColor(background);
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }
}
