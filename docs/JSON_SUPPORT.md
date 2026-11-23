# JSON support: proposed contract

## Status and scope

This document defines the output contract and its acceptance criteria (step 1).
The shared `FormDefinition` and an optional strict Draft 2020-12 exporter are
implemented, along with the RJSF UI schema adapter and a customer example endpoint.
Frontend components are not implemented yet. HTML signatures are preserved; intentional metadata default
corrections are described in the migration section.

Independent data kinds are now available in metadata and populated by built-in
handlers, runtime builders, and generated accessors. Typed default representation
and read-only choice conversion are available. Select and simple-checkbox defaults
are consistent across runtime and generated metadata. Constraint validation of
defaults and export-time diagnostics are implemented for the supported subset.

The generic API uses JSON Schema Draft 2020-12 with Formatic's neutral UI
contract version 1. The optional RJSF adapter uses its own UI schema format.
A later JSON Forms adapter will generate its own UI schema. OpenAPI export is a
separate future feature.

## Architecture and public boundary

The same renderer-neutral form definition will feed two independent branches:

```text
formatic-core: form definition and field metadata
  |-- HTML: Thymeleaf, JTE, Pebble
  `-- JSON Schema exporter
        `-- frontend adapter: schema + RJSF UI schema
```

The shared input is `com.formatic.core.form.FormDefinition`, containing an
optional title and field metadata. Existing metadata builders continue returning
`List<FormFieldMetadata>`:

```java
var definition = new FormDefinition("Contact", builder.buildMetadata(Contact.class));
```

The constructor preserves the prepared field order and copies the list into an
unmodifiable list. Null lists and null elements are rejected; a null title and an
empty field list are allowed. Metadata elements remain shared and mutable.
Consumers must not mutate them, particularly when they come from builder caches;
use metadata copies before making request-specific changes.

The JSON Schema exporter returns a structured JSON object, not HTML and not a
pre-serialized string. The optional RJSF frontend adapter combines it with a UI schema:

```java
public record JsonFormDefinition(
    ObjectNode schema,
    ObjectNode uiSchema
) {}
```

These are proposed Jackson types in optional JSON modules. Jackson must not
become a dependency of `formatic-core` or the HTML rendering API. A controller
serializes this result at the HTTP boundary.

## Using the JSON Schema exporter

In this repository's Gradle build:

```groovy
dependencies {
    implementation project(':formatic-schema-json')
}
```

Build metadata with your existing builder, then export it:

```java
import com.formatic.core.form.FormDefinition;
import com.formatic.schema.json.DefaultJsonSchemaExporter;

var definition = new FormDefinition("Contact", builder.buildMetadata(Contact.class));
var schema = new DefaultJsonSchemaExporter().export(definition);
```

The result is a Jackson `ObjectNode`. A Spring controller can return it directly,
or the application can serialize it with its configured `ObjectMapper`. The
module owns Jackson and NetworkNT validation dependencies; core and HTML rendering dependencies are
unchanged. The exporter does not mutate field metadata or options, and every
call returns a fresh tree.

Current support:

| Surface | Implemented behavior |
|---|---|
| Value kinds | String, integer, number, boolean, scalar-item arrays |
| Controls | AUTO, TEXT, TEXTAREA, NUMBER, RANGE, SELECT, RADIO, CHECKBOX, HIDDEN, PASSWORD, EMAIL, URL, PHONE, COLOR, DATE, TIME, DATETIME_LOCAL |
| Constraints | Explicit numeric bounds, string lengths, typed choices, array uniqueness |
| Structure | Dotted paths become objects; required leaves require ancestors |
| Descriptive metadata | Labels, help text, read-only annotations |
| Defaults | Typed or legacy conversion, checked against bounds, lengths, choices, and uniqueness |
| Diagnostics | `JsonSchemaExportException` includes the field path and failing feature |

Arrays require a multiple select or checkbox with a known scalar item type.
Numeric controls require integer/number data kinds. Other controls do not infer
types or formats from their names. No implicit browser limits, Java numeric
range limits, or Bean Validation rules are added.

Explicit `step` and nonempty `pattern` are currently rejected; equivalent
translations are not implemented. File and label controls are not yet supported.
Explicit temporal bounds/step fail export, even on supported temporal controls.
Alpha color input is unsupported. A password with an explicit default fails rather than
exposing that value.

String control mappings now include:

| Control | Data constraint |
|---|---|
| EMAIL | String with `format: email` |
| URL | String with `format: uri` (absolute URI, not necessarily HTTP) |
| PHONE | String, without an invented universal phone-number rule |
| COLOR | Six-digit hex string, such as `#aB12fF` |
| DATE | `format: date` plus a calendar-valid date pattern for years 0001-9999 |
| TIME | Local `HH:mm[:ss[.fraction]]`, with 1-3 fractional digits |
| DATETIME_LOCAL | Calendar-valid date plus `T` and local time; no timezone |

All these controls require a string data kind. Local time/date-time use standard
JSON Schema patterns instead of `time`/`date-time` formats, which describe
different timezone semantics. Patterns reject invalid hours, calendar dates,
leap days, offsets, and trailing newlines. Support is deliberately limited to
four-digit positive years and millisecond precision; extended HTML years and
other representations are not claimed. Default browser stepping is not added
as a schema constraint.

Email/URI formats follow JSON Schema validation semantics rather than claiming
exact browser-input parity. Clients must explicitly enable format assertions in
their validator. Server export uses NetworkNT Draft 2020-12 with format assertions
enabled to check defaults and choices against the generated control constraints.
This validator dependency is confined to the optional schema module. Export does
not replace server-side submission validation.

Declared option providers must have a nonempty prepared option list; unresolved
or empty provider results fail. The exporter never invokes providers. Static
empty choices without a provider currently mean no choice constraint.

HTML-only and UI presentation properties are not part of this data-only output.
For example, groups, disabled state, placeholders, and display conditions are
not translated by the JSON Schema exporter. The future UI adapter must handle
or reject them explicitly before claiming to export a complete frontend form.
This distinction means data schema export alone is not a frontend UI definition.

Do not make JSON export implement the existing HTML `FormRenderer`. Preserve
`render(FormRenderModel)` and its request-local values and errors. Neither the
schema nor UI schema includes submitted data, server errors, submission actions,
CSRF tokens, or success messages; applications transport these separately.

Labels and help text in the exported document are already localized. Locale
selection belongs to the metadata preparation stage. HTML themes do not select
JSON Schema types or frontend widgets.

## Data contract

Every schema has `"$schema": "https://json-schema.org/draft/2020-12/schema"` and
an object root with `properties`. Include a root `title` only when supplied.
Do not generate an `$id` without an application-supplied identity.

The initial contract permits unknown properties: omit `additionalProperties`.
Server binding and validation remain responsible for accepted submission fields.
A strict unknown-property mode is not part of the initial implementation.

Data types must be explicit in the prepared metadata, independently of the HTML
control type. Do not guess integer versus decimal from `step`, or number versus
string from the text of an option. The metadata work in steps 3-5 must supply
this information before export is reliable.

`FormFieldMetadata.dataType` uses `FormDataType` (`STRING`, `INTEGER`, `NUMBER`,
`BOOLEAN`, `ARRAY`). `itemDataType` describes scalar array items. Both are nullable:
null means unknown or not applicable, not a nullable JSON value. Nested object
structure continues to come from dotted paths rather than an object-valued leaf.

Runtime and generated builders share scalar mappings: integral primitives,
wrappers, and `BigInteger` become integers; floating-point types and `BigDecimal`
become numbers; booleans remain booleans. Strings, characters, enum names,
supported Java time types, and UUIDs become strings without asserting a format.
Arrays and collections become arrays; item types are inferred for scalar array
components and known collection declarations with concrete scalar type arguments.
Raw collections, wildcards, generic items, and custom collection item mappings
remain unknown. Maps and arbitrary object-valued fields need explicit mappings.

Custom runtime handlers can set `dataType` and `itemDataType` explicitly;
inference does not replace a non-null `dataType`. Direct built-in handler calls
also infer the type. Manually created metadata remains unspecified until the
caller sets its data kind. These additions do not change widget types, string
options/defaults, numeric bounds, or existing HTML rendering.

| Value kind | Schema representation |
|---|---|
| Text | `type: string` |
| Integral number | `type: integer` |
| Decimal number | `type: number` |
| Boolean checkbox | `type: boolean` |
| Nested DTO | `type: object` with nested `properties` |
| Multiple selection | `type: array` with typed `items` and `uniqueItems: true` |

The initial contract uses non-null JSON types. Optional means the property can
be omitted, not that `null` is accepted. Nullable types and arbitrary collection
or map schemas are deferred. Unsupported Java types require an explicit
mapping or an export error.

### Required properties and nested paths

`required` is an array on the containing object, not a boolean on a property.
It means property presence; it does not imply a nonempty string, a nonempty
selection, or a boolean value of `true`.

Convert the existing dotted binding path `address.city` to nested object
properties. JSON property names follow binding paths, not HTML IDs or CSS
selectors. Reject duplicate paths, empty path segments, and leaf/object
collisions such as both `address` and `address.city`.

For this initial contract, a required leaf also requires each ancestor object.
An optional leaf alone does not require its ancestors. This is an explicit
JSON export rule, not a change to existing Spring nested binding or validation.
Optional parent objects with conditionally required children need richer
metadata and are deferred.

### Constraints, defaults, and choices

| Metadata | Export rule |
|---|---|
| Numeric `min`, `max` | Emit `minimum`, `maximum` when present; preserve zero |
| String `minLength`, `maxLength` | Emit corresponding keywords when present |
| Label, help text | Emit `title`, `description` when supplied |
| Read-only | Emit the `readOnly` annotation; it is not access control |
| Choices | Emit typed `oneOf` entries with `const` values and label `title` |
| Multiple choices | Put choice constraints inside `items` |
| Default | Emit a correctly typed `default` only when explicitly present |

Reuse `FormConstraintValidator` for metadata validation. Never serialize NaN or
infinity. Numeric annotations already use `Double.NaN` for absent bounds;
length annotations use `-1`. Preserve those conventions initially, and address
the inconsistent defaults of manually constructed metadata before export.

Defaults must satisfy the generated schema. They are annotations, not a
guarantee that a validator fills missing data. Invalid defaults or option values
are errors with the field path, not values silently coerced or omitted.
Explicit empty-string defaults must be distinguishable from absent defaults.

The implemented `FormDataValue` holds a declared kind and a validated value:
`String`, `BigInteger`, `BigDecimal`, `Boolean`, or an unmodifiable list of scalar
`FormDataValue` items. It does not introduce a JSON dependency. For example:

```java
field.setTypedDefaultValue(new FormDataValue(FormDataType.STRING, ""));
```

`FormFieldValues.defaultValue(field)` prefers an explicit typed default and
checks its type against field/item metadata. Otherwise it converts a nonempty
legacy default string. Legacy null/empty strings mean absent. An explicit typed
default is independent of the HTML default string; HTML rendering does not read
it. Clear it with `setTypedDefaultValue(null)` to return to legacy conversion.
Array defaults must be supplied explicitly rather than splitting an ambiguous
comma-delimited string. Empty arrays also require a known item kind.

`FormFieldValues.optionValues(field)` converts the existing string choices using
the declared field kind (or array item kind), preserves their order, and rejects
duplicate typed values. Decimal choices such as `1.0` and `1.00` are duplicates.
Labels, selection flags, and original strings are not modified. Selected flags
do not implicitly create defaults. Provider resolution and option/default
membership checks belong to the upcoming exporter.

Text conversion accepts strict JSON numeric syntax without trimming, accepts
only lowercase `true` and `false` for booleans, and keeps integers/decimals at
arbitrary precision. Invalid conversions identify the field and value role.
Unknown types are rejected when conversion is requested, not during HTML
metadata construction.

Annotation default preparation follows these rules:

- Select defaults are now copied into the legacy default string by runtime and
  generated builders, matching radio defaults. Array defaults still require an
  explicit typed value rather than interpreting the string as a delimited list.
- For boolean/Boolean and simple string checkboxes, `defaultChecked=true` now
  produces `"true"` in metadata. The typed conversion is boolean or string
  according to the actual data kind. The default annotation value `false`
  leaves the metadata default absent; it does not imply an explicit JSON default.
- Collection checkbox defaults are not inferred from `defaultChecked`, because
  it does not identify selected items. Supply a typed array default explicitly.
- Password annotation defaults remain suppressed in runtime and generated
  metadata. The future exporter must also prevent explicit password defaults
  from being disclosed; annotation parity is not permission to export secrets.

These rules retain the existing inability to distinguish an omitted annotation
`defaultValue` from an explicitly supplied empty string at runtime. Use
`typedDefaultValue` when an explicit empty default is needed.

Resolve dynamic options before exporting. A declared unresolved provider is an
error; it must not become an unconstrained field. Duplicate typed choice values
are rejected. Selected options can supply a default only under a documented,
unambiguous selection rule.

## Presentation contract

### Framework-neutral UI contract

`DefaultJsonFormExporter` in `formatic-schema-json` exports
`com.formatic.schema.json.JsonFormDefinition`, serialized as `{schema, ui}`:

```java
var result = new DefaultJsonFormExporter().export(definition);
```

```json
{
  "schema": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "properties": { "name": { "type": "string", "minLength": 2 } }
  },
  "ui": {
    "version": 1,
    "fields": [
      {
        "path": "name",
        "widget": "text",
        "label": "Name",
        "placeholder": "Your name",
        "disabled": false,
        "readOnly": false
      }
    ]
  }
}
```

This is a Formatic contract, not a universal UI schema standard. React, Angular,
and Vue applications receive the same document and translate widget intent to
their own components. RJSF, Formly, and JSON Forms do not consume this neutral
`ui` object directly; frontend adapters are still needed.

Version 1 rules:

- `version` is required; consumers must reject versions they do not support.
- `fields` is an ordered array, retaining prepared field order, including
  interleaved nested fields. Empty forms use `fields: []`.
- `path` is the exact dotted binding path, not an HTML ID or JSON Pointer.
  Resolve `address.street` against nested data and schema properties. Path
  segments must be traversed as own properties, not evaluated as expressions.
  Unlike RJSF, neutral paths do not reserve `ui:` property prefixes.
- `widget` describes control intent: `text`, `textarea`, `number`, `range`,
  `select`, `multiselect`, `radio`, `checkbox`, `checkboxes`, `hidden`, `password`,
  `email`, `url`, `phone`, `color`, `date`, `time`, or `datetime-local`.
  AUTO resolves to text/number/checkbox using data kind.
- `label`, `placeholder`, and `helpText` are plain text, never executable HTML.
  Label is omitted when absent; placeholder/help text are omitted when empty.
- `disabled` and `readOnly` are always booleans. They describe the control,
  not authorization or a guarantee that the field is excluded from submission.
- Positive textarea `rows` is optional. No framework CSS, component names,
  JavaScript expressions, HTML IDs, or raw HTML attributes are exported.
- Types, required rules, bounds, choices/labels, and defaults remain in
  `schema`. Choices use `oneOf` on the property or array items. UI does not
  duplicate these validation rules.

Consumers must preserve JSON value types and local date/time strings. A framework
adapter unable to preserve a widget's semantics should report an unsupported
feature, not silently substitute a different value type or convert to UTC.
Choice widgets require prepared choices; single checkboxes require booleans.
Range widgets require explicit bounds. Groups, display conditions, option
groups/disabled/selected flags, cols/wrap, labels as standalone controls, and
files are not supported in version 1 and fail export. Existing HTML-only
properties are deliberately excluded.

**API response change:** `/api/forms/customer` now returns `{schema, ui}`, not
the earlier `{schema, uiSchema}` RJSF response. Clients must read `ui.version`
and adapt `ui.fields`; simply renaming `uiSchema` is not sufficient. Applications
that specifically want RJSF output can still call `RjsfFormExporter` in the
optional module and expose a separate explicitly library-specific route.
Existing HTML endpoints and both data-only/RJSF exporters are unchanged.

### Spring example endpoint

Run `./gradlew :formatic-example:bootRun`, then request:

```bash
curl -H 'Accept-Language: fr' http://localhost:8080/api/forms/customer
```

`GET /api/forms/customer` returns the combined neutral `{schema, ui}` definition
for the existing `CustomerForm` DTO. Marked field labels are localized through
Spring's `MessageSource` on metadata copies, without changing builder caches.
The title and literal labels remain unchanged. The route also works beneath an
application context path.

This is an example application route, not a library-provided generic endpoint.
It does not accept arbitrary DTO class names or export submitted data. Existing
HTML customer routes are unchanged. A JSON submission route and frontend
demonstration are not implemented yet; do not POST JSON to the existing
HTML form-binding routes. Required-property schemas also do not automatically
replace the DTO's Bean Validation rules.

### Using the RJSF adapter

Add `implementation project(':formatic-schema-rjsf')` in this repository's build.
This includes the data schema module transitively:

```java
import com.formatic.schema.rjsf.RjsfFormExporter;

var definition = new FormDefinition("Contact", builder.buildMetadata(Contact.class));
var result = new RjsfFormExporter().export(definition);
// result.schema() and result.uiSchema() are fresh, request-local JSON objects.
```

The returned `com.formatic.schema.rjsf.JsonFormDefinition` serializes as
`{"schema": {...}, "uiSchema": {...}}`. It does not include data, action URLs,
or errors. Export is read-only; mutating the result does not mutate metadata or
subsequent exports.

Widget names and type compatibility were checked against the
[RJSF 5.24.13 widget map](https://github.com/rjsf-team/react-jsonschema-form/blob/v5.24.13/packages/utils/src/getWidget.tsx).
An executable frontend demonstration and browser validation remain subsequent
steps. Clients must use a Draft 2020-12-capable validator with format assertions.

### Mapping and limitations

RJSF-specific output belongs in `uiSchema`, not JSON Schema. Preserve field
order using `ui:order` at each object level. Use prepared field order rather than
sorting labels or property names.

| Control or presentation | Initial RJSF mapping |
|---|---|
| Text | `ui:widget: text` |
| Textarea | `ui:widget: textarea` |
| Number | `ui:widget: updown` |
| Range | `ui:widget: range`, when its semantics are supported |
| Single select | `ui:widget: select` |
| Radio | `ui:widget: radio` |
| Boolean checkbox | `ui:widget: checkbox` |
| Checkbox choices | `ui:widget: checkboxes` with an array schema |
| Hidden input | Data property plus `ui:widget: hidden` |
| Placeholder | `ui:placeholder` |
| Help text | `ui:help` |
| Disabled | `ui:disabled` |
| Read-only | `ui:readonly` |
| Password | `ui:widget: password`; schema defaults remain prohibited |
| Email / URL | `ui:widget: email` / `uri` |
| Color / date | `ui:widget: color` / `date` |
| Phone, local time/date-time | `ui:widget: text`, preserving their string values |
| Textarea rows | `ui:options.rows` |

The adapter uses plain text for local time/date-time deliberately:
RJSF 5's [DateTimeWidget](https://github.com/rjsf-team/react-jsonschema-form/blob/v5.24.13/packages/core/src/components/widgets/DateTimeWidget.tsx)
converts values to/from UTC, while its
[TimeWidget](https://github.com/rjsf-team/react-jsonschema-form/blob/v5.24.13/packages/core/src/components/widgets/TimeWidget.tsx)
appends seconds. Neither reliably preserves the supported local string contract.
A dedicated frontend widget can be added later; native time/date-time pickers
are not claimed by this adapter.

Choice widgets require prepared options. Single checkboxes require booleans;
legacy string checkboxes fail rather than sending a different JSON value kind.
Text and textarea widgets require strings. AUTO chooses text/updown/checkbox by
data kind. Range widgets require explicit bounds; explicit stepping remains
unsupported by the data exporter.

The first occurrence of each nested path determines its object's position;
children retain their prepared relative order. Names starting with `ui:` are
rejected at any nesting level because they conflict with RJSF presentation keys.
Groups, display conditions, option groups, disabled options, and selected option
flags are rejected. Supply explicit typed/legacy defaults rather than selected
flags. `cols` and `wrap` are not supported; textarea rows are supported. HTML-only
CSS and raw attributes remain intentionally excluded.

CSS classes, outer CSS classes, raw HTML attributes, and preformatted HTML
attribute strings are HTML-only and are deliberately not exported. This
exclusion is not a validation failure. A frontend library chooses its own theme.

`LABEL`, groups, option groups, disabled individual options, and display
conditions need explicit frontend presentation support. Until supported, report
them as unsupported rather than silently producing a different interface.
Do not execute `displayCondition` strings as JavaScript or inject raw HTML.
Arbitrary `extraProperties` are not automatically exposed in JSON.

## Semantics that need special treatment

Some HTML constraints do not have a direct standard JSON Schema equivalent:

- HTML `step` may be relative to a nonzero stepping base. Do not map it blindly
  to `multipleOf`. A supported translation must preserve the base and be tested;
  otherwise reject an explicit step in the initial exporter.
- HTML patterns validate a whole value and use browser regex semantics.
  JSON Schema patterns are not implicitly anchored. Export only a documented,
  compatible subset with equivalent matching; reject untranslatable patterns.
- Standard Draft 2020-12 has no date/time minimum or maximum keyword.
  Reject explicit temporal bounds or stepping unless a supported extension is
  deliberately introduced.
- `date` can describe a supported date string. HTML `datetime-local` has no
  timezone and must not automatically become JSON Schema `date-time`.
  HTML time and URI/email rules also need explicit compatibility tests.
- JSON Schema `format` is not necessarily asserted by validators. Configure
  and verify the frontend and test validator rather than claiming automatic
  format enforcement.
- Files need an upload or reference protocol. File inputs are unsupported
  initially; do not serialize browser file objects as ordinary strings.
- Java Bean Validation annotations are not automatically exported merely
  because Formatic metadata is exported. A separate mapping is needed to claim
  server/client validation parity.

An unsupported widget, active constraint, unresolved option source, or invalid
metadata must fail export with a diagnostic containing the field path and the
unsupported feature. Do not return a partial success-shaped definition.
HTML-only properties excluded above are the documented exception.

## Reference output

This example defines required text, an explicit zero numeric minimum, typed
radio choices, multiple selection, and a nested required field:

```json
{
  "schema": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Contact",
    "type": "object",
    "properties": {
      "name": { "type": "string", "minLength": 2, "maxLength": 20 },
      "age": { "type": "integer", "minimum": 0, "maximum": 120 },
      "contactMethod": {
        "type": "string",
        "oneOf": [
          { "const": "email", "title": "Email" },
          { "const": "phone", "title": "Phone" }
        ]
      },
      "interests": {
        "type": "array",
        "uniqueItems": true,
        "items": {
          "type": "string",
          "oneOf": [
            { "const": "java", "title": "Java" },
            { "const": "web", "title": "Web" }
          ]
        }
      },
      "address": {
        "type": "object",
        "properties": { "city": { "type": "string", "minLength": 1 } },
        "required": ["city"]
      }
    },
    "required": ["name", "address"]
  },
  "uiSchema": {
    "ui:order": ["name", "age", "contactMethod", "interests", "address"],
    "name": { "ui:widget": "text", "ui:placeholder": "Your name" },
    "age": { "ui:widget": "updown" },
    "contactMethod": { "ui:widget": "radio" },
    "interests": { "ui:widget": "select" },
    "address": {
      "ui:order": ["city"],
      "city": { "ui:widget": "text" }
    }
  }
}
```

For this definition, `{"name":"Jo","age":0,"address":{"city":"Paris"}}` is valid.
Names shorter than two characters, negative ages, fractional ages, missing
addresses, and duplicate interests are invalid. Optional properties may be
omitted; explicit `null` values are invalid.

## HTML compatibility and migration

Step 1 changed documentation only. Constraint defaults are now corrected
(step 4): newly constructed metadata uses `Double.NaN` for absent `min`, `max`,
and `step`, and `-1` for absent `minLength` and `maxLength`. Annotation defaults
already use these sentinels. Primitive getter/setter signatures are unchanged.

**Intentional HTML behavior change:** manually created numeric fields no longer
emit accidental `min="0"`, `max="0"`, or `step="0"` constraints, and manually
created text fields no longer emit an accidental `maxlength="0"`. Browsers use
their own defaults when attributes are omitted (including range control bounds
and default numeric stepping). This is not equivalent to disabling all browser
constraints.

Applications relying on previous implicit zeros must set their intended limits:

```java
var amount = new FormFieldMetadata("amount", FormFieldType.NUMBER, "Amount", 1);
amount.setMin(0);   // Explicit zero is still a real constraint.
amount.setMax(100);
amount.setStep(1);  // Use a positive step; zero is not a valid numeric step.

var text = new FormFieldMetadata("name", FormFieldType.TEXT, "Name", 2);
text.setMaxLength(0); // Explicitly preserve an empty-only HTML input if intended.
```

To clear bounds again, set numeric values to `Double.NaN` and length limits to
`-1`. Explicit `minLength=0` remains metadata and can be exported to JSON;
existing templates may omit this redundant HTML lower bound.
Do not change constraints by comparing getters to zero to detect absence.
Raw metadata serializers must also account for NaN according to their own JSON
configuration; the future schema exporter will omit absent constraints.

Remaining compatibility considerations:

**Default extraction correction:** previously ignored `SelectInput.defaultValue`
and simple `CheckboxInput.defaultChecked=true` now populate metadata. Unbound
standalone HTML controls therefore select/check their defaults; bound values
still take precedence. Remove the select default or use `defaultChecked=false`
to retain the former unselected/unchecked initial view. Spring MVC controls use
their existing binding rules; initialize the backing DTO when a bound initial
value is desired rather than assuming metadata overrides it.

| Proposed change | Potential HTML break and cause | Solution |
|---|---|---|
| Align metadata defaults with NaN / -1 (implemented) | Manually constructed fields previously started with primitive zero; HTML attributes are now omitted | Set intended zero limits explicitly as above; built-in annotation sentinel defaults are unchanged |
| Replace primitive bounds with nullable getters | Source/binary compatibility and template unboxing failures | Prefer existing sentinels initially; if nullable getters become necessary, provide a migration and update all templates together |
| Add typed options and defaults | HTML currently consumes string option values and defaults | Preserve existing HTML accessors; add typed metadata separately and adapt it explicitly at output boundaries |
| Add independent data types | Custom handlers and generated metadata may omit new information | Update every built-in creation path; give custom handlers an explicit mapping contract; do not require JSON-only metadata for HTML rendering |
| Add data-type JavaBean getters (implemented) | Applications serializing metadata directly may expose new `dataType` and `itemDataType` properties | Use an explicit response DTO or application serializer configuration for a stable legacy payload; the proposed schema response is separate from raw metadata |
| Add `typedDefaultValue` getter (implemented) | Raw metadata serialization may expose the new property; the independent schema default is not used by HTML | Use a stable response DTO for legacy payloads; continue setting `defaultValue` for HTML and set the typed default explicitly for JSON |
| Add `FormDefinition` | Replacing `FormRenderModel` would lose values, errors, action, and hidden fields | Keep existing render methods; add compatible conveniences rather than replacing request state |
| Translate dotted paths into objects | Changing HTML names would break nested form submission | Nest only the JSON projection; keep HTML binding names and configured IDs unchanged |

Before any later implementation changes HTML behavior, record the old and new
behavior, explain why the correction is necessary, and provide a migration
example. Cover standalone Thymeleaf/JTE/Pebble and Spring MVC templates, not just
the JSON exporter. Do not claim compatibility from Java compilation alone.

## Acceptance criteria for subsequent implementation

1. Validate exported schemas against Draft 2020-12 and validate actual sample
   instances, including the reference cases above.
2. Test absent bounds versus explicit zero, typed choices/defaults, invalid
   defaults, required semantics, nested path collisions, and unresolved options.
3. Verify metadata parity across reflection, bytecode, generated accessors, and
   copies without mutating cached metadata during export.
4. Assert diagnostic paths and feature names for every unsupported mapping.
5. Render the reference definition in the chosen RJSF release with a validator
   configured for Draft 2020-12; verify widgets, ordering, and submission shape.
6. Preserve HTML output and binding except for separately explained and tested
   corrections. Keep server-side submission validation authoritative.

Next implementation step: add a frontend adapter for the neutral document,
a demonstration with Draft 2020-12 validation, and a typed JSON submission endpoint.
