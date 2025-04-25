<?php

use classes\Position;
use service\PositionService;

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/PositionService.php';
    create();
}

function create() {
    $data = json_decode(file_get_contents("php://input"), true);
    if (isset($data['latitude']) && isset($data['longitude']) && isset($data['date']) && isset($data['imei'])) {
        $latitude = $data['latitude'];
        $longitude = $data['longitude'];
        $date = $data['date'];
        $imei = $data['imei'];
        $ss = new PositionService();
        $ss->create(new Position(1, $latitude, $longitude, $date, $imei));

        echo json_encode([
            "status" => "success",
            "message" => "Position created successfully!"
        ]);
    } else {
        echo "Missing required fields!";
    }
}
