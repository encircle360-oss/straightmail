<@spring.message "email.subject"/>: <b>${subject!""}</b><br/>
<@spring.message "email.text"/>:
<p>
    ${text!""}<br>
    ${number?string}<br>
    ${string}<br>
    ${bool?string("yes","no")}<br>
    ${object.singleString}
    ${object.singleDouble?string.currency}
</p>

