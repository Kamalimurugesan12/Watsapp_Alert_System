package com.waalert.whatsapp_alert_backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST /api/whatsapp/send-template
 *
 * template : "Dear {{name}}, your salary for {{month}} is ₹{{amount}}"
 * rows     : list of maps — each map is one DB row (column → value)
 * phoneKey : which key in the map contains the phone number
 *            (optional — backend auto-detects if not supplied)
 */
@Data
public class TemplateSendRequest {

    private String template;

    /** Each element is one row: { "name": "Kamali", "mob_no": "918610256725", "amount": 26444 } */
    private List<Map<String, Object>> rows;

    /** Optional: name of the column that holds the phone number */
    private String phoneKey;
}