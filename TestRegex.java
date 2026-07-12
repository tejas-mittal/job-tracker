public class TestRegex {
    public static void main(String[] args) {
        String html = "<html><head><style> hr { background: #fff; } </style></head><body><a href=\"https://teams.microsoft.com/xyz\">Join meeting</a></body></html>";
        html = html.replaceAll("(?i)(?s)<head.*?</head>", " ")
                   .replaceAll("(?i)(?s)<style[^>]*>.*?</style>", " ")
                   .replaceAll("(?i)(?s)<script[^>]*>.*?</script>", " ");
        html = html.replaceAll("(?i)<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", "$2 $1");
        html = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").replaceAll("&[a-zA-Z0-9#]+;", " ").trim();
        System.out.println(html);
    }
}
