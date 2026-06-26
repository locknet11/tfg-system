from flask import Flask, request
from jinja2 import Template

app = Flask(__name__)


@app.route("/")
def index():
    name = request.args.get("name", "World")
    # Vulnerable: user input is concatenated directly into the template string
    # When name={{7*7}}, template becomes "Hello {{7*7}}" which renders "Hello 49"
    tpl = Template("Hello " + name)
    return tpl.render()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)
