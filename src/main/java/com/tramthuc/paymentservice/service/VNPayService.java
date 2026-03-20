package com.tramthuc.paymentservice.service;

import com.tramthuc.paymentservice.config.VNPayConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;
    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;
    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;
    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    public String createOrderPaymentUrl(Long orderId, long amount, String ipAddr) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_IpAddr = ipAddr != null ? ipAddr : "127.0.0.1";

        long vnp_Amount = amount * 100; // VNPay quy định số tiền phải nhân lên 100 lần
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(vnp_Amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", String.valueOf(orderId)); // Mã đơn hàng trong DB của ta
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Lấy thời gian hiện tại để làm ngày tạo GD
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        // Thiết lập thời gian hết hạn (15 phút)
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sắp xếp các tham số theo bảng chữ cái
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        
        try {
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Tạo chuỗi Hash
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Tạo chuỗi Query params URL
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                         .append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            
            // Dùng thuật toán HMAC_SHA512 với secret key để ký điện tử
            String vnp_SecureHash = VNPayConfig.hmacSHA512(vnpHashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);
            
            // Trả về link full
            return vnpPayUrl + "?" + query.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL thanh toán VNPay", e);
        }
    }

    public boolean verifyPayment(jakarta.servlet.http.HttpServletRequest request) {
        try {
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    fields.put(fieldName, fieldValue);
                }
            }
            
            // Lấy chữ ký do VNPay gửi về và xóa khỏi map để tự tính toán lại
            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");
            
            // Sắp xếp và hash
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = fields.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            
            // So sánh chữ ký ta tự tính với chữ ký VNPay gửi về
            String signValue = VNPayConfig.hmacSHA512(vnpHashSecret, hashData.toString());
            return signValue.equals(vnp_SecureHash);
        } catch (Exception e) {
            return false;
        }
    }
}