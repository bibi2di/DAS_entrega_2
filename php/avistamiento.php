<?php
require 'config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $usuario_id = $_POST['usuario_id'];
    $especie = $_POST['especie'];
    $latitud = $_POST['latitud'];
    $longitud = $_POST['longitud'];
    $imagen = $_POST['imagen'];

    $imagen_decodificada = base64_decode($imagen);
    $nombreArchivo = uniqid() . ".jpg";
    $rutaArchivo = "fotos/" . $nombreArchivo;

    if (file_put_contents($rutaArchivo, $imagen_decodificada) === false) {
        echo "Error al guardar la imagen en el servidor.";
        exit;
    }

    $stmt = $conn->prepare("INSERT INTO avistamientos (usuario_id, especie, latitud, longitud, imagen) VALUES (?, ?, ?, ?, ?)");
    $stmt->bind_param("isdds", $usuario_id, $especie, $latitud, $longitud, $nombreArchivo);
    $stmt->execute();

    if ($stmt->affected_rows > 0) {
        echo "Avistamiento guardado correctamente.";
    } else {
        echo "Error al guardar el avistamiento.";
    }
}
?>
