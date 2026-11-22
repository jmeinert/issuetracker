import { createTheme, Input } from '@mantine/core';

export const theme = createTheme({
  defaultRadius: 'xl',
  components: {
    InputWrapper: Input.Wrapper.extend({
      defaultProps: {
        withAsterisk: false,
      },
    }),
  },
});
