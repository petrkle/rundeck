<template>
  <div id="fieldcustomeditor" class="col-sm-12">
    <hr />

    <div v-if="customFields != null">
      <div
        v-for="(field, index) in customFields"
        :key="index"
        :class="['form-group']"
        data-testid="field-item"
      >
        <label class="col-sm-2 control-label input-sm">{{
          field.label || field.key
        }}</label>
        <div class="col-sm-9">
          <input
            v-model="field.value"
            type="text"
            :class="['form-control', 'input-sm', 'context_var_autocomplete']"
            size="100"
            @change="changeField(field)"
            :data-testid="'field-input-' + index"
          />
        </div>
        <div class="col-sm-1">
          <span
            class="btn btn-xs btn-default"
            :title="$t('message_delete')"
            @click="removeField(field)"
            data-testid="remove-field-button"
          >
            <i class="glyphicon glyphicon-remove"></i
          ></span>
        </div>
        <div v-if="field.desc" class="col-sm-10 col-sm-offset-2 help-block">
          <div class="help-block" data-testid="field-description-help">
            {{ field.desc }}
          </div>
        </div>
      </div>
    </div>

    <btn
      type="primary"
      @click="openNewField()"
      data-testid="add-field-button"
      >{{ $t("message_addField") }}</btn
    >

    <modal
      id="modal-demo"
      ref="modal"
      v-model="modalAddField"
      title="Add Field"
      ok-text="Save"
      :backdrop="true"
      :dismiss-btn="true"
      :keyboard="true"
      cancel-text="Close"
      append-to-body
      data-testid="modal-title"
    >
      <div class="row" style="padding-left: 30px !important">
        <alert
          v-if="duplicate"
          type="warning"
          data-testid="duplicate-warning"
          ref="duplicateWarningRef"
          ><b>Warning!</b> {{ $t("message_duplicated") }}.</alert
        >

        <div class="col-md-10">
          <div v-if="useOptions" class="form">
            <div :class="['form-data']">
              <label class="col-md-4">{{ $t("message_select") }}</label>
              <div class="col-md-8">
                <vue-multiselect
                  v-model="selectedField"
                  :options="customOptions"
                  track-by="label"
                  label="label"
                  :placeholder="$t('message_select')"
                  data-testid="multiselect"
                >
                </vue-multiselect>
              </div>
            </div>

            <div :class="['form-data']">
              <label class="col-md-4">{{ $t("message_description") }}</label>
              <div class="col-md-8">
                <input
                  v-model="newFieldDescription"
                  type="text"
                  :class="['form-control']"
                  data-testid="field-description-input"
                />
                <div class="help-block">{{ $t("message_empty") }}</div>
              </div>
            </div>
          </div>

          <div v-if="!useOptions" class="form">
            <div :class="['form-group']">
              <label class="col-md-4">{{ $t("message_fieldLabel") }}</label>
              <div class="col-md-8">
                <input
                  v-model="newLabelField"
                  type="text"
                  :class="['form-control']"
                />
              </div>
            </div>
            <div :class="['form-group']">
              <label class="col-md-4">{{ $t("message_fieldKey") }}</label>
              <div class="col-md-8">
                <input
                  v-model="newField"
                  type="text"
                  :class="['form-control']"
                  data-testid="field-key-input"
                />
              </div>
            </div>

            <div :class="['form-group']">
              <label class="col-md-4">{{ $t("message_description") }}</label>
              <div class="col-md-8">
                <input
                  v-model="newFieldDescription"
                  type="text"
                  :class="['form-control']"
                  data-testid="field-description-input"
                />
                <div class="help-block">{{ $t("message_empty") }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div>
          <button
            type="button"
            class="btn btn-default reset_page_confirm"
            @click="modalAddField = false"
            data-testid="cancel-button"
          >
            {{ $t("message_cancel") }}
          </button>

          <button
            type="button"
            class="btn btn-cta reset_page_confirm"
            @click="addField()"
            data-testid="confirm-add-field-button"
          >
            {{ $t("message_add") }}
          </button>
        </div>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import VueMultiselect from "vue-multiselect";
import { defineComponent } from "vue";
import { Btn, Alert, Modal } from "uiv";

interface CustomField {
  key?: string;
  label?: string;
  value?: string;
  desc?: string;
}

export default defineComponent({
  name: "DynamicFormPluginProp",
  components: {
    VueMultiselect,
    Btn,
    Alert,
    Modal,
  },
  props: {
    fields: {
      type: String,
      required: true,
    },
    options: {
      type: String,
      required: false,
    },
    element: {
      type: String,
      required: true,
    },
    hasOptions: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      customFields: [] as CustomField[],
      customOptions: [] as any[],
      useOptions: false,
      modalAddField: false,
      duplicate: false,
      newField: "",
      newLabelField: "",
      newFieldDescription: "",
      selectedField: { value: "", label: "" },
    };
  },
  mounted() {
    if (this.hasOptions === "true") {
      this.useOptions = true;
    }
    if (this.fields != null && this.fields !== "") {
      const customFieldsObject = JSON.parse(this.fields);
      if (customFieldsObject != null) {
        const fields = Object.keys(customFieldsObject).map((key: any) => {
          const value = customFieldsObject[key];
          if (value.desc == null) {
            value.desc = "Field key: " + value.key;
          }
          return value;
        });
        this.customFields = fields;
      }
    }

    if (this.useOptions && this.options !== null && this.options !== "") {
      const optionsObject = JSON.parse(this.options);
      const options = Object.keys(optionsObject).map((key: any) => {
        const data = optionsObject[key];
        return { value: key, label: data };
      });
      this.customOptions = options;
    }
  },
  beforeUnmount() {
    this.customFields = null as any;
  },
  methods: {
    openNewField() {
      this.modalAddField = true;
    },
    addField() {
      let field = {} as CustomField;
      this.duplicate = false;

      if (this.useOptions) {
        if (this.selectedField !== null) {
          const newField = this.selectedField;

          let description = this.newFieldDescription;
          if (description == "") {
            description = "Field key " + newField.value;
          } else {
            description = description + " (Field key: " + newField.value + ")";
          }

          field = {
            key: newField.value,
            label: newField.label,
            desc: description,
          };
        }
      } else {
        let description = this.newFieldDescription;
        if (description == "") {
          description = "Field key " + this.newField;
        } else {
          description = description + " (Field key: " + this.newField + ")";
        }

        field = {
          key: this.newField,
          label: this.newLabelField,
          value: "",
          desc: description,
        };
      }
      let exists = false;
      this.customFields.forEach((row: any) => {
        if (field.key === row.key) {
          exists = true;
        }
      });

      if (!exists) {
        this.customFields.push(field);
        this.newField = "";
        this.newLabelField = "";
        this.newFieldDescription = "";
        this.modalAddField = false;
        this.refreshPlugin();
      } else {
        this.duplicate = true;
      }
    },
    removeField(row: any) {
      const fields = [] as CustomField[];
      this.customFields.forEach((field: CustomField) => {
        if (field.key !== row.key) {
          fields.push(field);
        }
      });
      this.customFields = fields;
      this.refreshPlugin();
    },
    changeField(field: CustomField) {
      this.refreshPlugin();
    },
    refreshPlugin() {
      const fieldsJson = JSON.stringify(this.customFields);
      const cFields = document.getElementById(this.element) as HTMLInputElement;
      cFields.value = fieldsJson;
      this.$emit("update:modelValue", fieldsJson);
    },
  },
});
</script>
<style scoped src="vue-multiselect/dist/vue-multiselect.min.css"></style>

<style scoped>
.form-data {
  padding-bottom: 30px;
  margin-bottom: 20px;
}
</style>
