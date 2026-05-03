package com.recetea.infrastructure.reports.openpdf;

import com.recetea.core.recipe.application.ports.out.report.IRecipeReportPort;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
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
import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;

/**
 * OpenPDF 3.x implementation of {@link IRecipeReportPort}. Renders a
 * recipe to a printable technical sheet and returns the bytes for the
 * caller to write through a {@code FileChooser} dialog.
 *
 * <p><b>Strict-grayscale aesthetic</b> — only {@link Color#BLACK},
 * {@link Color#WHITE}, {@link Color#GRAY}, {@link Color#DARK_GRAY},
 * {@link Color#LIGHT_GRAY} are used. The Recetea brand palette lives in
 * the JavaFX UI layer only; the PDF stays brand-neutral so the same
 * sheet template can be reused if the UI palette ever shifts.
 *
 * <p><b>Metadata hardening.</b> {@code Title}, {@code Author}, and
 * {@code Creator} are all set to {@code I18n.get("app.name")} ("Recetea")
 * <em>before</em> {@code doc.open()} — OpenPDF silently ignores metadata
 * changes after open, so this ordering is contractual. The blanket
 * "Recetea" attribution prevents leaking OS account names, JDK install
 * paths, or recipe-specific user data into the file metadata.
 *
 * <p><b>Buffer pre-sizing.</b> The {@code ByteArrayOutputStream} is
 * pre-sized at 8 KiB to skip the 7-9 internal reallocations a default-
 * sized BAOS would do during typical sheet synthesis. The final
 * {@code toByteArray()} copy is the only allocation that escapes the
 * method.
 *
 * <p><b>ES — </b>Implementación de {@link IRecipeReportPort} con
 * OpenPDF 3.x. Renderiza una receta a una ficha técnica imprimible
 * y devuelve los bytes para que el llamador los escriba a través de
 * un diálogo {@code FileChooser}.
 *
 * <p><b>Estética estricta en escala de grises</b> — sólo se usan
 * {@link Color#BLACK}, {@link Color#WHITE}, {@link Color#GRAY},
 * {@link Color#DARK_GRAY}, {@link Color#LIGHT_GRAY}. La paleta de
 * marca de Recetea vive sólo en la capa de UI JavaFX; el PDF se
 * mantiene neutro de marca para que la misma plantilla de ficha
 * pueda reutilizarse si la paleta de UI cambia.
 *
 * <p><b>Endurecimiento de metadatos.</b> {@code Title},
 * {@code Author} y {@code Creator} se establecen todos a
 * {@code I18n.get("app.name")} ("Recetea") <em>antes</em> de
 * {@code doc.open()} — OpenPDF ignora silenciosamente los cambios
 * de metadatos después de open, así que este orden es contractual.
 * La atribución uniforme a "Recetea" evita filtrar nombres de
 * cuenta del SO, rutas de instalación del JDK o datos del usuario
 * específicos de la receta en los metadatos del archivo.
 *
 * <p><b>Predimensionado del buffer.</b> El
 * {@code ByteArrayOutputStream} se predimensiona a 8 KiB para
 * saltarse las 7-9 reasignaciones internas que un BAOS de tamaño
 * por defecto haría durante la síntesis típica de la ficha. La
 * copia final de {@code toByteArray()} es la única asignación que
 * se escapa del método.
 */
public class OpenPdfRecipeAdapter implements IRecipeReportPort {

    private static final Font FONT_TITLE   = new Font(Font.HELVETICA, 20, Font.BOLD,   Color.BLACK);
    private static final Font FONT_SECTION = new Font(Font.HELVETICA, 12, Font.BOLD,   Color.BLACK);
    private static final Font FONT_HEADER  = new Font(Font.HELVETICA, 10, Font.BOLD,   Color.BLACK);
    private static final Font FONT_BODY    = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

    /**
     * Initial buffer capacity for the recipe-sheet BAOS. A typical sheet
     * (header + meta table + ~10 ingredients + ~6 steps + footer) lands
     * around 4-8 KB; 8 KiB skips the 7-9 internal reallocations that would
     * occur from the 32-byte default and keeps the byte[] copy on
     * {@code toByteArray()} cheap.
     */
    private static final int RECIPE_PDF_INITIAL_CAPACITY = 8 * 1024;

    @Override
    public byte[] generateTechnicalSheet(Recipe recipe, String authorUsername) {
        // Pre-sized BAOS avoids power-of-two reallocations during write-heavy
        // PDF synthesis. try-with-resources documents stream-resource intent
        // even though BAOS.close() is a no-op (buffer is GC-managed).
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(RECIPE_PDF_INITIAL_CAPACITY)) {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            try {
                PdfWriter.getInstance(doc, out);
                // Generic, brand-only metadata set BEFORE doc.open(): OpenPDF
                // ignores metadata changes after open. Prevents leaking OS
                // account names, JDK paths, or recipe-specific user data.
                String app = I18n.get("app.name");
                doc.addTitle(app);
                doc.addAuthor(app);
                doc.addCreator(app);
                doc.open();

                addHeader(doc, recipe);
                addMetaTable(doc, recipe, authorUsername);
                addIngredientsTable(doc, recipe);
                addSteps(doc, recipe);
                addSocialFooter(doc, recipe);

            } catch (DocumentException e) {
                throw new InfrastructureException("Failed to generate recipe technical sheet PDF", e);
            } finally {
                if (doc.isOpen()) doc.close();
            }
            // Final byte[] copy is the only allocation that escapes the method;
            // BAOS itself becomes GC-eligible the instant this return completes.
            return out.toByteArray();
        } catch (java.io.IOException e) {
            // Unreachable: ByteArrayOutputStream.close() is documented as a no-op.
            // Required catch because OutputStream.close() declares throws IOException.
            throw new InfrastructureException("Unexpected I/O failure closing recipe PDF buffer", e);
        }
    }

    // ── Sections ──────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Recipe recipe) throws DocumentException {
        Paragraph title = new Paragraph(recipe.getTitle(), FONT_TITLE);
        title.setSpacingAfter(14);
        doc.add(title);
    }

    private void addMetaTable(Document doc, Recipe recipe, String authorUsername) throws DocumentException {
        doc.add(sectionParagraph(I18n.get("report.recipe.section.meta")));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 7f});
        table.setSpacingAfter(14);

        // Author row first — mirrors the on-screen detail view ordering.
        // Null username (account deleted) → localised fallback.
        String displayName = (authorUsername != null && !authorUsername.isBlank())
                ? authorUsername
                : I18n.get("recipe.detail.author.unknown");
        addMetaRow(table, I18n.get("field.label.author"),         displayName);
        addMetaRow(table, I18n.get("report.recipe.meta.prepTime"), recipe.getPreparationTimeMinutes().value() + " min");
        addMetaRow(table, I18n.get("report.recipe.meta.servings"), String.valueOf(recipe.getServings().value()));
        if (recipe.getDescription() != null && !recipe.getDescription().isBlank()) {
            addMetaRow(table, I18n.get("report.recipe.meta.description"), recipe.getDescription());
        }
        addMetaRow(table, I18n.get("field.label.category"),       recipe.getCategory().name());
        addMetaRow(table, I18n.get("field.label.difficulty"),     recipe.getDifficulty().name());
        doc.add(table);
    }

    private void addIngredientsTable(Document doc, Recipe recipe) throws DocumentException {
        doc.add(sectionParagraph(I18n.get("report.recipe.section.ingredients")));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2f, 6f});
        table.setSpacingAfter(14);

        addPlainHeader(table, I18n.get("column.quantity"), I18n.get("column.unit"), I18n.get("column.ingredient"));

        for (RecipeIngredient ing : recipe.getIngredients()) {
            String qty  = ing.quantity().stripTrailingZeros().toPlainString();
            String unit = ing.unitAbbreviation()  != null ? ing.unitAbbreviation()  : "";
            String name = ing.ingredientName()    != null ? ing.ingredientName()    : "";
            for (String text : new String[]{qty, unit, name}) {
                PdfPCell cell = bodyCell(text);
                cell.setHorizontalAlignment(text.equals(qty) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                table.addCell(cell);
            }
        }
        doc.add(table);
    }

    private void addSteps(Document doc, Recipe recipe) throws DocumentException {
        doc.add(sectionParagraph(I18n.get("report.recipe.section.steps")));

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
        footer.add(new Chunk(I18n.get("report.recipe.footer.avgScore") + " ",      FONT_SECTION));
        footer.add(new Chunk(avg + " / 5     ",                                   FONT_BODY));
        footer.add(new Chunk(I18n.get("report.recipe.footer.totalRatings") + " ", FONT_SECTION));
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
        labelCell.setBackgroundColor(Color.LIGHT_GRAY);
        labelCell.setPadding(5);
        labelCell.setBorderColor(Color.DARK_GRAY);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_BODY));
        valueCell.setPadding(5);
        valueCell.setBorderColor(Color.DARK_GRAY);
        table.addCell(valueCell);
    }

    private void addPlainHeader(PdfPTable table, String... labels) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, FONT_HEADER));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(Color.DARK_GRAY);
            cell.setBorderWidth(Rectangle.UNDEFINED);
            table.addCell(cell);
        }
    }

    private PdfPCell bodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(5);
        cell.setBorderColor(Color.DARK_GRAY);
        return cell;
    }
}
