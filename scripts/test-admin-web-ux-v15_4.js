// UX regression on the existing Web Admin page. No network credentials or live writes.
"use strict";
const fs = require("node:fs");
const vm = require("node:vm");
const path = require("node:path");
const assert = require("node:assert/strict");

function element(id) {
  return {
    id, value: "", textContent: "", hidden: false, children: [], listeners: {},
    focused: false,
    setAttribute() {},
    focus() { this.focused = true; },
    addEventListener(type, listener) { this.listeners[type] = listener; },
    replaceChildren(...children) { this.children = children; },
    append(...children) { this.children.push(...children); }
  };
}

async function run() {
  const html = fs.readFileSync(path.join(__dirname, "../admin-web/index.html"), "utf8");
  const source = html.match(/<script>([\s\S]*?)<\/script>/);
  assert.ok(source);
  const nodes = new Map();
  const get = id => {
    if (!nodes.has(id)) nodes.set(id, element(id));
    return nodes.get(id);
  };
  let confirmResult = false;
  let confirmQuestion = "";
  let failRpc = false;
  let unconfirmedRpc = false;
  const calls = [];
  const context = vm.createContext({
    document: {getElementById: get, createElement: element},
    confirm(question) { confirmQuestion = question; return confirmResult; },
    fetch: async (url, options) => {
      assert.equal(options.method, "POST");
      calls.push(String(url));
      if (failRpc) return {ok: false, status: 403, text: async () => ""};
      return {ok: true, status: 200, text: async () =>
        unconfirmedRpc ? "false" :
          String(url).endsWith("/rpc/musicflow_manage_role")
            ? '{"roles":["platform_admin"]}' : "true"};
    }
  });
  new vm.Script(source[1], {filename: "admin-web/index.html"}).runInContext(context);
  const evaluate = text => vm.runInContext(text, context);
  evaluate('session={url:"https://qa.supabase.co",key:"sb_publishable_qa",token:"qa-session"}');
  evaluate('access={roles:["owner"],permissions:[]};selected={id:"qa-user",email:"test@example.com"}');
  get("reason").value = "Kiểm thử phân quyền";
  get("role").value = "platform_admin";

  await evaluate('perform(() => change("role",true))');
  assert.equal(calls.length, 0, "Cancel must not call any RPC");
  assert.match(get("notice").textContent, /Đã hủy thao tác/);
  assert.doesNotMatch(get("notice").textContent, /hoàn thành/);
  assert.match(confirmQuestion, /Quản trị viên nền tảng/);
  console.log("WEB_ADMIN_CANCEL_NOT_SUCCESS=PASS");

  confirmResult = true;
  await evaluate('perform(() => change("role",true))');
  assert.ok(calls.some(url => url.endsWith("/rpc/musicflow_manage_role")));
  assert.match(get("notice").textContent, /hoàn thành/);
  console.log("WEB_ADMIN_SUCCESS_AFTER_RPC=PASS");

  failRpc = true;
  await evaluate('perform(() => change("role",true))');
  assert.match(get("notice").textContent, /từ chối/);
  assert.doesNotMatch(get("notice").textContent, /hoàn thành/);
  console.log("WEB_ADMIN_FAILED_RPC_NOT_SUCCESS=PASS");

  calls.length = 0;
  get("reason").value = "";
  get("reason").focused = false;
  await evaluate('perform(() => change("role",true))');
  assert.equal(get("reason").focused, true);
  assert.match(get("notice").textContent, /Lý do|lý do/);
  assert.equal(calls.length, 0);
  console.log("WEB_ADMIN_MISSING_REASON_FOCUSED=PASS");

  get("reason").value = "Kiểm tra xác nhận máy chủ";
  failRpc = false;
  unconfirmedRpc = true;
  await evaluate('perform(() => change("role",true))');
  assert.match(get("notice").textContent, /Máy chủ chưa xác nhận/);
  assert.doesNotMatch(get("notice").textContent, /hoàn thành/);
  console.log("WEB_ADMIN_UNCONFIRMED_RPC_NOT_SUCCESS=PASS");
  console.log("WEB_ADMIN_UX_V15_4=5_PASS");
}

run().catch(error => {console.error(error);process.exitCode = 1;});
