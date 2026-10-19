module ni.edu.uam.concurrenciaparejas {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.concurrenciaparejas to javafx.fxml;
    exports ni.edu.uam.concurrenciaparejas;
}