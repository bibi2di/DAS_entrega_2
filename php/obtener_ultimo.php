<?php
require 'config.php';

$stmt = $conn->prepare("SELECT id, especie, latitud, longitud, imagen, fecha, usuario_id FROM avistamientos ORDER BY fecha DESC LIMIT 1");
$stmt->execute();
$result = $stmt->get_result();
$avistamiento = $result->fetch_assoc();

if ($avistamiento) {
    $avistamiento['fecha_formateada'] = date("d/m/Y H:i", strtotime($avistamiento['fecha']));
    header('Content-Type: application/json');
    echo json_encode($avistamiento);
} else {
    header('Content-Type: application/json');
    echo json_encode(null);
}

$conn->close();
?>