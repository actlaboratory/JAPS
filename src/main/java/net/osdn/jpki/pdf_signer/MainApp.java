package net.osdn.jpki.pdf_signer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.osdn.jpki.pdf_signer.control.LicenseDialog;
import net.osdn.jpki.wrapper.JpkiException;
import net.osdn.jpki.wrapper.JpkiWrapper;
import net.osdn.util.javafx.application.SingletonApplication;
import net.osdn.util.javafx.concurrent.Async;
import net.osdn.util.javafx.event.SilentEventHandler;
import net.osdn.util.javafx.fxml.Fxml;
import net.osdn.util.javafx.scene.control.Dialogs;
import net.osdn.util.javafx.scene.control.pdf.Pager;
import net.osdn.util.javafx.scene.control.pdf.PdfView;
import net.osdn.util.javafx.stage.StageUtil;
import javafx.scene.control.TextInputDialog;

import org.actlab.updater.UpdateChecker;
import org.actlab.updater.UpdaterStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MainApp extends SingletonApplication implements Initializable {

    public static final String APPLICATION_NAME = "JPKI ACCESSIBLE PDF SIGNER";
    public static final String APPLICATION_SHORT_NAME = "JAPS";
    public static final String APPLICATION_VERSION;
    private static final String SOFTWARE_PAGE_URL = "https://actlab.org/software/JAPS";

    static {
        System.setProperty(
                "org.apache.commons.logging.LogFactory", "net.osdn.jpki.pdf_signer.LogFilter");
        LogFilter.setLevel("org.apache.pdfbox", LogFilter.Level.ERROR);
        LogFilter.setLevel("org.apache.fontbox", LogFilter.Level.ERROR);

        int[] version = Datastore.getApplicationVersion();
        if(version != null) {
            if (version[2] == 0) {
                APPLICATION_VERSION = String.format("%d.%d", version[0], version[1]);
            } else {
                APPLICATION_VERSION = String.format("%d.%d.%d", version[0], version[1], version[2]);
            }
        } else {
            APPLICATION_VERSION = "";
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/app-icon-48px.png")));
        primaryStage.titleProperty().bind(new StringBinding() {
            {
                bind(inputFileProperty);
            }
            @Override
            protected String computeValue() {
                try {
                    return (inputFileProperty.get() != null ? inputFileProperty.get().getCanonicalPath() + " - " : "")
                            + APPLICATION_NAME + " " + APPLICATION_VERSION;
                } catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });

        primaryStage.showingProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue == true && newValue == false) {
                Platform.exit();
            }
        });

        Parent root = Fxml.load(this);

        Scene scene = new Scene(root);
        scene.setOnDragOver(wrap(this::scene_onDragOver));
        scene.setOnDragDropped(wrap(this::scene_onDragDropped));
        scene.getAccelerators().putAll(pager.createDefaultAccelerators());

        StageUtil.setRestorable(primaryStage, Preferences.userNodeForPackage(getClass()));
        primaryStage.setOnShown(event -> { Platform.runLater(wrap(this::stage_onReady)); });
        primaryStage.setMinWidth(448.0);
        primaryStage.setMinHeight(396.0);
        primaryStage.setOpacity(0.0);
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread.currentThread().setUncaughtExceptionHandler(handler);
    }

    protected Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            showException(e);
        }
    };

    protected void showException(Throwable exception) {
        exception.printStackTrace();

        Runnable r = ()-> {
            String title;
            if(exception instanceof JpkiException) {
                title = "エラー";
            } else {
                title = exception.getClass().getName();
            }
            String message = exception.getLocalizedMessage();
            if(message != null) {
                message = message.trim();
            }
            showAlert(title, message);
        };
        if(Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    @FXML MenuItem            menuFileOpen;
    @FXML MenuItem            menuFileSave;
    @FXML MenuItem            menuFileExit;
    @FXML MenuItem            menuHelpAbout;
    @FXML MenuItem            menuHelpUpdate;
    @FXML Pager               pager;
    @FXML PdfView             pdfView;
    @FXML ImageView           ivCursor;
    @FXML ProgressIndicator   piSign;
    @FXML Button              btnRemoveSignature;
    @FXML Button              btnEditSignature;
    @FXML Button              btnAddSignature;
    @FXML ListView<Signature> lvSignature;
    ObjectBinding<Signature>  signatureBinding;
    ObjectProperty<File>      inputFileProperty = new SimpleObjectProperty<File>();
    ObjectProperty<File>      signedTemporaryFileProperty = new SimpleObjectProperty<File>();
    BooleanProperty           busyProperty = new SimpleBooleanProperty();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lvSignature.setCellFactory(new SignatureListCell.Factory(this));

        //
        // event handlers
        //
        menuFileOpen.setOnAction(wrap(this::menuFileOpen_onAction));
        menuFileSave.setOnAction(wrap(this::menuFileSave_onAction));
        menuFileExit.setOnAction(wrap(this::menuFileExit_onAction));
        menuHelpAbout.setOnAction(wrap(this::menuHelpAbout_onAction));
        menuHelpUpdate.setOnAction(wrap(this::menuHelpUpdate_onAction));
        pdfView.setOnMouseMoved(wrap(this::pdfView_onMouseMoved));
        pdfView.setOnMouseClicked(wrap(this::pdfView_onMouseClicked));
        btnRemoveSignature.setOnAction(wrap(this::btnRemoveSignature_onAction));
        btnEditSignature.setOnAction(wrap(this::btnEditSignature_onAction));
        btnAddSignature.setOnAction(wrap(this::btnAddSignature_onAction));

        //
        // bindings
        //
        signatureBinding = Bindings
                .when(lvSignature.getSelectionModel().selectedItemProperty().isNotNull())
                .then(lvSignature.getSelectionModel().selectedItemProperty())
                .otherwise(Signature.EMPTY);

        menuFileSave.disableProperty().bind(signedTemporaryFileProperty.isNull());

        pager.maxPageIndexProperty().bind(pdfView.maxPageIndexProperty());
        pager.pageIndexProperty().bindBidirectional(pdfView.pageIndexProperty());

        pdfView.cursorProperty().bind(Bindings
                .when(pdfView.documentProperty().isNotNull()
                        .and(ivCursor.visibleProperty())
                        .and(ivCursor.imageProperty().isNotNull()))
                .then(Cursor.NONE)
                .otherwise(Cursor.DEFAULT));

        ivCursor.imageProperty().bind(Bindings.select(signatureBinding, "image"));
        ivCursor.scaleXProperty().bind(
                pdfView.renderScaleProperty().multiply(Bindings.selectDouble(signatureBinding, "imageScaleX")));
        ivCursor.scaleYProperty().bind(
                pdfView.renderScaleProperty().multiply(Bindings.selectDouble(signatureBinding, "imageScaleY")));
        ivCursor.visibleProperty().bind(Bindings
                .selectBoolean(signatureBinding, "visible")
                .and(pdfView.hoverProperty()));

        btnRemoveSignature.disableProperty().bind(
                Bindings.not(Bindings.selectBoolean(signatureBinding, "visible")));
        btnEditSignature.disableProperty().bind(
                Bindings.not(Bindings.selectBoolean(signatureBinding, "visible")));

        piSign.visibleProperty().bind(busyProperty);
    }

    void stage_onReady() {
        getPrimaryStage().setOpacity(1.0);

        lvSignature.getItems().clear();
        lvSignature.getItems().add(Signature.INVISIBLE);
        Async.execute(() -> {
            return Datastore.loadSignatures();
        }).onSucceeded(signatures -> {
            for (Signature signature : signatures) {
                lvSignature.getItems().add(signature);
            }
            Platform.runLater(() -> {
                checkJpkiAvailability();
            });
        }).onFailed(exception -> {
            showException(exception);
        });
        lvSignature.setOnKeyPressed(SilentEventHandler.wrap(this::lvSignature_onKeyPressed));
    }

    void scene_onDragOver(DragEvent event) {
        if(isAcceptable(getFile(event))) {
            event.acceptTransferModes(TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    void scene_onDragDropped(DragEvent event) {
        File file = getFile(event);
        if(isAcceptable(file)) {
            getPrimaryStage().toFront();
            signedTemporaryFileProperty.set(null);
            inputFileProperty.set(file);
            pdfView.load(file);
            event.setDropCompleted(true);
        }
        event.consume();
    }

    void menuFileOpen_onAction(ActionEvent event) {
        Preferences preferences = Preferences.userNodeForPackage(getClass());

        FileChooser fc = new FileChooser();
        fc.setTitle("開く");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        String lastOpenDirectory = preferences.get("lastOpenDirectory", null);
        if(lastOpenDirectory != null) {
            File dir = new File(lastOpenDirectory);
            if(dir.isDirectory()) {
                fc.setInitialDirectory(dir);
            }
        }
        File file = fc.showOpenDialog(getPrimaryStage());
        if(file != null) {
            preferences.put("lastOpenDirectory", file.getParentFile().getAbsolutePath());
            if(isAcceptable(file)) {
                signedTemporaryFileProperty.set(null);
                inputFileProperty.set(file);
                pdfView.load(file);
            }
        }
    }

    void menuFileSave_onAction(ActionEvent event) throws IOException {
        String defaultName = inputFileProperty.get().getName();
        int i = defaultName.lastIndexOf('.');
        if(i > 0) {
            defaultName = defaultName.substring(0, i);
        }
        defaultName += "-signed.pdf";

        FileChooser fc = new FileChooser();
        fc.setTitle("名前を付けて保存");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialDirectory(inputFileProperty.get().getParentFile());
        fc.setInitialFileName(defaultName);

        File file = fc.showSaveDialog(getPrimaryStage());
        if(file != null) {
            Files.copy(signedTemporaryFileProperty.get().toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showAlert("保存完了", "下記のパスに保存しました。\n\n" + file.getPath());
        }
    }

    void menuFileExit_onAction(ActionEvent event) {
        getPrimaryStage().close();
    }

    void menuHelpAbout_onAction(ActionEvent event) throws IOException {
        String license = Datastore.getLicense();
        LicenseDialog dialog = new LicenseDialog(getPrimaryStage(), APPLICATION_NAME + " " + APPLICATION_VERSION, license);
        dialog.showAndWait();
    }

    void menuHelpUpdate_onAction(ActionEvent event) throws IOException {
        UpdateChecker checker = new UpdateChecker(APPLICATION_SHORT_NAME,getSoftwareVersion());
        UpdaterStatus result = checker.check();
        switch(result) {
            case FAIL:
                showAlert("アップデートチェック", checker.getMessage());
                return;
            case ALREADY_UPDATED:
                showAlert("アップデートチェック", "最新版を利用中です。更新の必要はありません。");
                return;
            case PENDING:
                showAlert("アップデートチェック", checker.getMessage() + "\n\nブラウザでダウンロードページを開きますので、最新版をダウンロードしてご利用ください。\n\n更新内容は、以下の通りです。" + checker.getResponse().getUpdate_description());
                openBrowser(SOFTWARE_PAGE_URL);
                return;
            case SEE_WEB:
                showAlert("アップデートチェック", checker.getMessage() + "ブラウザでページを表示します。");
                openBrowser(checker.getResponse().getURL());
        }
        System.out.println(checker.check());
    }

    void btnAddSignature_onAction(ActionEvent event) throws IOException {
        SignatureDialog dialog = new SignatureDialog(getPrimaryStage(), null);
        Signature newSignature = dialog.showAndWait().orElse(null);
        if(newSignature == null) {
            return;
        }
        lvSignature.getItems().add(newSignature);
        Datastore.saveSignatures(lvSignature.getItems().subList(0, lvSignature.getItems().size()));
    }

    void btnEditSignature_onAction(ActionEvent event) throws IOException {
        Signature currentSignature = lvSignature.getSelectionModel().getSelectedItem();
        if(currentSignature == null || currentSignature.getImage() == null) {
            return;
        }
        SignatureDialog dialog = new SignatureDialog(getPrimaryStage(), currentSignature);
        Signature newSignature = dialog.showAndWait().orElse(null);
        if(newSignature == null) {
            return;
        }
        lvSignature.getItems().add(lvSignature.getSelectionModel().getSelectedIndex(), newSignature);
        lvSignature.getItems().remove(lvSignature.getSelectionModel().getSelectedIndex());
        Datastore.saveSignatures(lvSignature.getItems().subList(0, lvSignature.getItems().size()));
    }

    void btnRemoveSignature_onAction(ActionEvent event) throws IOException {
        Signature currentSignature = lvSignature.getSelectionModel().getSelectedItem();
        if(currentSignature == null || currentSignature.getImage() == null) {
            return;
        }
        ButtonType result = Dialogs.showConfirmation(getPrimaryStage(),
                "印影の削除",
                currentSignature.getTitle() + " を削除しますか？");
        if(result != ButtonType.YES) {
            return;
        }

        lvSignature.getItems().remove(lvSignature.getSelectionModel().getSelectedIndex());
        lvSignature.getSelectionModel().clearSelection();
        Datastore.saveSignatures(lvSignature.getItems().subList(0, lvSignature.getItems().size()));
    }

    public void lvSignature_onKeyPressed(KeyEvent event) throws JpkiException, IOException, ReflectiveOperationException {
        // エンターキー以外は無視
        if(event.getCode() != KeyCode.ENTER){
            return;
        }
        
        Signature signature = lvSignature.getFocusModel(). getFocusedItem();
        if(signature != Signature.INVISIBLE) {
            showAlert(
                    "可視署名の実行",
                    "キー操作での可視署名には対応していません。\n" +
                    "可視署名を行うには、使用したい署名を表示位置までマウスでドラッグする必要があります。"
                );
            return;
        }
        signInvisible();
    }

    public void lvSignature_cell_onMousePressed(MouseEvent event) throws JpkiException, IOException, ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        ListCell<Signature> cell = (ListCell<Signature>)event.getSource();

        //空のセルをクリックしたときにリストビューの選択を解除します。
        if(cell.isEmpty()) {
            lvSignature.getSelectionModel().clearSelection();
            return;
        }

        Signature signature = cell.getItem();
        if(signature != Signature.INVISIBLE) {
            return;
        }

        try {
            if(!event.isPrimaryButtonDown()) {
                return;
            }
            signInvisible();
        } finally {
            lvSignature.getSelectionModel().clearSelection();
        }
    }

    private void signInvisible(){
        if(pdfView.getDocument() == null) {
            showAlert(
                    "ファイルが開かれていません",
                    "PDFファイルを開いてから実行してください。"
                    );
        } else if(checkJpkiAvailability()) {
            PDDocument document = pdfView.getDocument();
            int pageIndex = pdfView.getPageIndex();
            SignatureOptions options = null;
            busyProperty.set(true);
            Async.execute(() -> sign(document, null, APPLICATION_NAME, APPLICATION_VERSION))
                .onSucceeded(tmpFile -> {
                    if(tmpFile != null) {
                        signedTemporaryFileProperty.set(tmpFile);
                        pdfView.load(tmpFile, pageIndex);
                        busyProperty.set(false);

                        if(ButtonType.YES == Dialogs.showConfirmation(getPrimaryStage(),
                                APPLICATION_NAME + " " + APPLICATION_VERSION,
                                "署名が完了しました。\nファイルに名前を付けて保存しますか？")) {
                            menuFileSave.fire();
                        }
                    }
                })
                .onCompleted(state -> busyProperty.set(false));
        }
    }

    public void lvSignature_cell_onMouseClicked(MouseEvent event) {
        // 左ダブルクリックでない場合は何もしない。
        if(event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) {
            return;
        }

        @SuppressWarnings("unchecked")
        ListCell<Signature> cell = (ListCell<Signature>)event.getSource();

        // 不可視署名では何もしない。
        Signature signature = cell.getItem();
        if(signature == Signature.INVISIBLE) {
            return;
        }

        if(cell.isEmpty()) {
            // 空のセルがダブルクリックされた場合は「新規」操作を発動します。
            btnAddSignature.fire();
        } else {
            // 可視署名がダブルクリックされた場合は「編集」操作を発動します。
            btnEditSignature.fire();
        }
    }

    void pdfView_onMouseMoved(MouseEvent event) {
        Image image = ivCursor.getImage();
        if(image != null) {
            ivCursor.setLayoutX(event.getX() - (int)(image.getWidth() / 2.0));
            ivCursor.setLayoutY(event.getY() - (int)(image.getHeight() / 2.0));
        }
    }

    void pdfView_onMouseClicked(MouseEvent event) throws  JpkiException, IOException, ReflectiveOperationException {
        // 必要な条件を満たしている場合、可視署名を実行します。

        if(event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        Rectangle2D renderBounds = pdfView.getRenderBounds();
        double x = event.getX() - renderBounds.getMinX();
        double y = event.getY() - renderBounds.getMinY();
        if(x < 0.0 || y < 0.0 || x > renderBounds.getWidth() || y > renderBounds.getHeight()) {
            return;
        }

        Signature signature = signatureBinding.get();
        if(!signature.isVisible()) {
            return;
        }

        if(!checkJpkiAvailability()) {
            return;
        }

        PDDocument document = pdfView.getDocument();
        int pageIndex = pdfView.getPageIndex();
        PDRectangle pageMediaBox = document.getPage(pageIndex).getMediaBox();
        double xPt = x * pageMediaBox.getWidth() / renderBounds.getWidth();
        double yPt = y * pageMediaBox.getHeight() / renderBounds.getHeight();

        PDVisibleSignDesigner designer;
        try(InputStream is = new FileInputStream(signature.getFile())) {
            designer = new PDVisibleSignDesigner(is);
            designer.width((float)mm2px(signature.getWidthMillis()));
            designer.height((float)mm2px(signature.getHeightMillis()));
            designer.xAxis((float)xPt - designer.getWidth() / 2);
            designer.yAxis((float)yPt - designer.getHeight() / 2 - pageMediaBox.getHeight());
        }
        PDVisibleSigProperties props = new PDVisibleSigProperties();
        props.setPdVisibleSignature(designer);
        props.visualSignEnabled(true);
        props.page(pageIndex + 1);
        props.buildSignature();

        SignatureOptions options = new SignatureOptions();
        options.setPage(pageIndex);
        options.setVisualSignature(props);

        lvSignature.getSelectionModel().clearSelection();
        busyProperty.set(true);
        Async.execute(() -> sign(document, options, APPLICATION_NAME, APPLICATION_VERSION))
            .onSucceeded(tmpFile -> {
                if(tmpFile != null) {
                    signedTemporaryFileProperty.set(tmpFile);
                    pdfView.load(tmpFile, pageIndex);
                    busyProperty.set(false);

                    if(ButtonType.YES == Dialogs.showConfirmation(getPrimaryStage(), APPLICATION_NAME + " " + APPLICATION_VERSION,
                            "署名が完了しました。\nファイルに名前を付けて保存しますか？")) {
                        menuFileSave.fire();
                    }
                    lvSignature.getSelectionModel().clearSelection();
                }
            })
            .onCompleted(state -> busyProperty.set(false));
    }

    protected File getFile(DragEvent event) {
        if(event.getDragboard().hasFiles()) {
            List<File> files = event.getDragboard().getFiles();
            if(files.size() == 1) {
                return files.get(0);
            }
        }
        return null;
    }

    protected boolean isAcceptable(File file) {
        return file != null && file.getName().matches("(?i).+(\\.pdf)");
    }

    protected boolean checkJpkiAvailability() {
        boolean isAvailable = JpkiWrapper.isAvailable();
        if(!isAvailable) {
            // JPKI 利用者クライアントソフトがインストールされていない場合、
            // インストールを促すために、クリックで公的個人認証サービスのウェブサイトが開くようにします。
            // ただし、Microsoft Storeアプリではストア以外でのアプリインストールを促すことが禁止されているため
            // UWPとして実行されている場合にはウェブサイトを開く機能を提供せずメッセージ表示のみに留めています。
            if(Datastore.isRunningAsUWP()) {
                showAlert("構成", "JPKI 利用者クライアントソフトが見つかりません。");
            } else {
                showAlert("事前準備", ""
                                + "JPKI 利用者クライアントソフトをインストールしてください。\n"
                                + "ブラウザーでダウンロードサイトを開きます。"
                        );
                openBrowser("https://www.jpki.go.jp/download/win.html");
            }
        }
        return isAvailable;
    }

    protected File sign(PDDocument document, SignatureOptions options, String applicationName, String applicationVersion) throws JpkiException, IOException, ReflectiveOperationException {
        File tmpFile = Datastore.getMyDataDirectory(true).resolve("output.tmp").toFile();
        try (OutputStream output = new FileOutputStream(tmpFile)) {
            output.flush();
            JpkiWrapper jpki = new JpkiWrapper();
            jpki.setApplicationName(applicationName);
            jpki.setApplicationVersion(applicationVersion);
            jpki.addSignature(output, document, options);
            return tmpFile;
        } catch(JpkiException e) {
            //ユーザーがキャンセル操作をした場合はダイアログを表示しません。
            if(e.getWinErrorCode() != JpkiException.SCARD_W_CANCELLED_BY_USER) {
                throw e;
            }
        }
        return null;
    }

    public static double mm2px(double mm) {
        return mm * 72.0 / 25.4;
    }

    private void showAlert(String title,String message) {
        try {
            LicenseDialog dialog = new LicenseDialog(
                    getPrimaryStage(),
                    title,
                    message);
            dialog.showAndWait();
        } catch (IOException e) {
            throw new InternalError(e);
        }
//        TextInputDialog d = new TextInputDialog(message);
//        d.getEditor().setEditable(false);
//        d.getEditor().setMinHeight(200);
//        d.getEditor().setMinWidth(500);
//        d.getEditor().
//        d.setTitle(title);
//        d.showAndWait();
    }
    
    private void openBrowser(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch(IOException e) {
            showAlert("エラー", "ブラウザの起動に失敗しました。代わりに、ブラウザを起動して下記のURLにアクセスしてください。\n\n" + url);
        }
    }
    
    private String getSoftwareVersion() {
        int[] intArray = Datastore.getApplicationVersion();
        return intArray[0] + "." + intArray[1] + "." +intArray[2];
    }
}

