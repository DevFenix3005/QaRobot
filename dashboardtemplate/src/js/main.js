import 'bootstrap'
import hljs from "highlight.js/lib/core";
import xml from "highlight.js/lib/languages/xml";
import javascript from "highlight.js/lib/languages/javascript";
import Icon from '../assets/img/robot.png';

import '../scss/style.scss';

hljs.registerLanguage('xml', xml);
hljs.registerLanguage('javascript', javascript);

document.addEventListener('DOMContentLoaded', function () {
    const aContant = document.querySelector("a#logo-img-content");
    const image = new Image(45, 45);
    image.classList.add("d-inline-block", "align-text-top");
    image.alt = "Logo";
    image.src = Icon;
    aContant.append(image, "QaRobot");

    const xmlContent = document.querySelector("code#xml-content");
    document.querySelectorAll("code.language-javascript").forEach((element) => {
        hljs.highlightElement(element);
    });
    hljs.highlightElement(xmlContent);
});

