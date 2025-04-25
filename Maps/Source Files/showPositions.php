<?php

use service\PositionService;

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/PositionService.php';
    showPositions();
}

function showPositions() {
    $cs = new PositionService();
    header('Content-type: application/json');
    echo json_encode(array("positions" => $cs->getAll()));
}
