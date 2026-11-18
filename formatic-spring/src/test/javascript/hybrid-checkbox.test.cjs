const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { test } = require('node:test');

const template = fs.readFileSync(path.join(__dirname,
    '../../main/resources/templates/bootstrap/forms/form-base-hybrid.html'), 'utf8');
const script = template.match(/<script th:inline="javascript">([\s\S]*?)<\/script>/)[1]
    .replace('[[${editorId}]]', '1');

for (const [name, value, options, expected] of [
    ['true boolean', true, ['true'], [true]],
    ['false boolean', false, ['true'], [false]],
    ['null boolean', null, ['true'], [false]],
    ['true string', 'true', ['true'], [true]],
    ['false string', 'false', ['true'], [false]],
    ['selected collection', ['sport', 'music'], ['sport', 'music', 'reading'], [true, true, false]],
    ['empty collection', [], ['sport', 'music'], [false, false]],
    ['numeric options', [1], ['1', '2'], [true, false]],
]) {
    test(`hybrid checkbox loading: ${name}`, async () => {
        const elements = options.map(value => ({ type: 'checkbox', value, checked: true }));
        let completion;
        vm.runInNewContext(script, {
            document: {
                querySelectorAll(selector) {
                    assert.equal(selector, '[name="checkbox"]');
                    return elements;
                },
            },
            fetch(url) {
                assert.equal(url, '/api/editors/1');
                const response = Promise.resolve({ json: () => ({ checkbox: value }) });
                completion = response;
                return {
                    then(callback) {
                        completion = response.then(callback);
                        return {
                            then(callback) {
                                completion = completion.then(callback);
                                return completion;
                            },
                        };
                    },
                };
            },
        });
        await completion;
        assert.deepEqual(elements.map(element => element.checked), expected);
        assert.ok(elements.every(element => options.includes(element.value)));
    });
}

test('hybrid nested DTO loading preserves dotted paths, zero and false', async () => {
    const controls = {
        'address.street': [{ type: 'text', value: '' }],
        'address.rating': [{ type: 'range', value: '' }],
        'address.newsletter': [{ type: 'checkbox', value: 'true', checked: true }],
        'address.interests': [{ type: 'checkbox', value: 'sport', checked: false }],
    };
    vm.runInNewContext(script, {
        document: {
            querySelectorAll(selector) {
                return controls[selector.match(/name="([^"]+)"/)[1]] || [];
            },
        },
        fetch: () => Promise.resolve({ json: () => ({
            address: { street: 'Main', rating: 0, newsletter: false, interests: ['sport'] },
        }) }),
    });
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(controls['address.street'][0].value, 'Main');
    assert.equal(controls['address.rating'][0].value, 0);
    assert.equal(controls['address.newsletter'][0].checked, false);
    assert.equal(controls['address.interests'][0].checked, true);
});
