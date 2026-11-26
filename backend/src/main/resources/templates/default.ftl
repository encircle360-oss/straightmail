<html>
<head>
    <style>
        body {
            font-family: sans-serif;
        }

        h3 {
            color: #FF9100FF;
        }
    </style>
</head>
<body>
<h3>
    <@spring.message "email.subject"/>: <b>${subject!""}</b>
</h3>
<div>
    <h4>
        <@spring.message "email.text"/>
    </h4>
    <p>
        ${text!""}
    </p>
</div>
</body>
</html>
