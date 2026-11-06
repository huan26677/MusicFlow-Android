// Dependency-free tests for internal admin-web access refresh and fail-closed UI.
// Runs entirely in a VM with fake network/DOM; no credentials or live writes.
"use strict";
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const assert = require("node:assert/strict");

function element(id) {
  return {
    id, hidden: false, value: "", textContent: "", className: "",
    children: [], listeners: {},
    addEventListener(event, listener) { this.listeners[event] = listener; },
    replaceChildren(...items) { this.children = items; },
    append(...items) { this.children.push(...items); },
    setAttribute() {}
  };
}

async function run() {
  const html = fs.readFileSync(path.join(__dirname, "../admin-web/index.html"), "utf8");
  const raw = html.match(/<script>([\s\S]*?)<\/script>/);
  assert.ok(raw, "Admin JavaScript must be embedded in the HTML");
  const elements = new Map();
  const get = id => {
    if (!elements.has(id)) elements.set(id, element(id));
    return elements.get(id);
  };
  let roleResponse = {roles: ["platform_admin"], permissions: ["manage_users"]};
  let networkFailure = false;
  const context = vm.createContext({
    document: {getElementById: get, createElement: tag => element(tag)},
    fetch: async (url, options) => {
      assert.equal(options.method, "POST");
      assert.ok(String(url).endsWith("/rest/v1/rpc/musicflow_my_access"));
      if (networkFailure) throw new Error("Simulated offline");
      return {ok: true, status: 200, text: async () => JSON.stringify(roleResponse)};
    }
  });
  new vm.Script(raw[1], {filename: "admin-web/index.html"}).runInContext(context);
  const call = expression => vm.runInContext(expression, context);
  const loginAsQA = () => call('session={url:"https://qa.supabase.co",key:"sb_publishable_qa",token:"temporary-session"}');

  loginAsQA();
  await call("refreshCurrentAccess()");
  assert.equal(get("users-panel").hidden, false);
  assert.equal(get("owner-panel").hidden, true);
  assert.equal(get("creators-panel").hidden, true);
  assert.equal(get("drafts-panel").hidden, true);
  assert.equal(get("tracks-panel").hidden, true);
  console.log("SCOPED_ADMIN_PANEL_VISIBILITY=PASS");

  get("user-list").children = [element("cached-user")];
  roleResponse = {roles: ["platform_admin"], permissions: []};
  await call("refreshCurrentAccess()");
  assert.equal(get("users-panel").hidden, true);
  assert.equal(get("user-list").children.length, 0);
  assert.equal(call("session!==null"), true);
  console.log("PERMISSION_REVOKE_CLEARS_STALE_USERS=PASS");

  roleResponse = {roles: [], permissions: []};
  await assert.rejects(call("refreshCurrentAccess()"), /không còn quyền/i);
  assert.equal(get("session-panel").hidden, true);
  assert.equal(get("login-panel").hidden, false);
  assert.equal(call("session===null"), true);
  console.log("ROLE_REVOKE_SIGNS_OUT=PASS");

  loginAsQA();
  networkFailure = true;
  await assert.rejects(call("refreshCurrentAccess()"), /Simulated offline/);
  assert.equal(call("session===null"), true);
  assert.equal(get("users-panel").hidden, true);
  console.log("NETWORK_FAILURE_FAIL_CLOSED=PASS");
  console.log("WEB_ROLE_REFRESH_TESTS=4_PASS");
}

run().catch(error => {console.error(error);process.exitCode = 1;});
