package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class ISSNEditor extends HBox implements FieldEditorFX {
    @FXML private ISSNEditorViewModel viewModel;
    @FXML private EditorTextField textField;
    @FXML private Button journalInfoButton;
    @FXML private Button fetchInformationByIdentifierButton;

    @Inject private DialogService dialogService;
    @Inject private GuiPreferences preferences;
    @Inject private KeyBindingRepository keyBindingRepository;
    @Inject private UndoManager undoManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private StateManager stateManager;

    private Optional<BibEntry> entry = Optional.empty();

    public ISSNEditor(Field field,
                      SuggestionProvider<?> suggestionProvider,
                      FieldCheckers fieldCheckers,
                      UndoAction undoAction,
                      RedoAction redoAction) {

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new ISSNEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                taskExecutor,
                dialogService,
                undoManager,
                stateManager,
                preferences);

        establishBinding(textField, viewModel.textProperty(), keyBindingRepository, undoAction, redoAction);
        textField.initContextMenu(new DefaultMenu(textField), keyBindingRepository);
        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
    }

    public ISSNEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        this.entry = Optional.of(entry);
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public void requestFocus() {
        textField.requestFocus();
    }

    @FXML
    private void fetchInformationByIdentifier() {
        entry.ifPresent(viewModel::fetchBibliographyInformation);
    }

    @FXML
    private void showJournalInfo() {
        if (JournalInfoOptInDialogHelper.isJournalInfoEnabled(dialogService, preferences.getEntryEditorPreferences())) {
            viewModel.showJournalInfo(journalInfoButton);
        }
    }
}
