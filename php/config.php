<?php
$host = "localhost";
$user = "Xbleon005"; 
$password = "j1LbYIg9"; 
$db = "Xbleon005_txorionak";

$conn = new mysqli($host, $user, $password, $db);
if ($conn->connect_error) {
    die("Conexión fallida: " . $conn->connect_error);
}
?>
