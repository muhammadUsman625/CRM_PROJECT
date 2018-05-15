package product.details;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import objects.ProductProperty;
import product.ProductDashController;
import product.newProduct.NewProductController;
import product.view.ProductViewController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductDetailsController implements Initializable {


    @FXML
    private Label txt_pname;
    @FXML
    private Label txt_price;
    @FXML
    private Label txt_startedOn;
    @FXML
    private TextArea txt_desc;
    @FXML
    private JFXButton btn_back;
    @FXML
    private JFXButton btn_edit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Image image = new Image(this.getClass().getResourceAsStream("/res/img/left-arrow.png"));
        btn_back.setGraphic(new ImageView(image));
        btn_back.setAlignment(Pos.CENTER_LEFT);
        btn_back.setTooltip(new Tooltip("Back to Products"));
        btn_back.setOnAction(event -> {
            try {
                ProductDashController.main_paneF.setCenter(
                        FXMLLoader.load(
                                getClass().getClassLoader().getResource("product/view/product_view.fxml")));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        ProductProperty product = ProductViewController.staticProduct;

        btn_edit.setOnAction(event -> {
            NewProductController.stInstance = 'U';
            try {
                ProductDashController.main_paneF.setCenter(
                        FXMLLoader.load(
                                getClass().getClassLoader().getResource("product/newProduct/new_product.fxml")));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        txt_pname.setText(product.getName());
        txt_price.setText(String.valueOf(product.getPrice()));
        txt_desc.setText(product.getDesc());
        txt_startedOn.setText(product.getFormattedDate());

    }
}
