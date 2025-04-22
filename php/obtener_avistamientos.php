<?php
require 'config.php';

$usuario_id = isset($_GET['usuario_id']) ? $_GET['usuario_id'] : null;

if ($usuario_id) {
    $stmt = $conn->prepare("SELECT id, especie, latitud, longitud, imagen, fecha FROM avistamientos WHERE usuario_id = ? ORDER BY fecha DESC");
    $stmt->bind_param("i", $usuario_id);
} else {
    $stmt = $conn->prepare("SELECT id, especie, latitud, longitud, imagen, fecha, usuario_id FROM avistamientos ORDER BY fecha DESC");
}

$stmt->execute();
$result = $stmt->get_result();

$avistamientos = array();
while ($row = $result->fetch_assoc()) {

    $fecha_formateada = date("d/m/Y H:i", strtotime($row['fecha']));
    $row['fecha_formateada'] = $fecha_formateada;

    $avistamientos[] = $row;
}

header('Content-Type: application/json');
echo json_encode($avistamientos);

$conn->close();
?>
